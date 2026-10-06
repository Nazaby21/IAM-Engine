-- Trusted in-process image processing identity. It has no password or role grants.
INSERT INTO app_user (id, kind, display_name)
VALUES ('f8a0dd81-a185-4ddb-916b-65b6f120c391', 'service', 'School image processor');

-- Reviewers need to read submitted schools, branches and images before deciding.
INSERT INTO role_permission (role_id, permission_code, scope)
SELECT r.id, p.code, 'platform'::access_scope
FROM role r CROSS JOIN permission p
WHERE r.code = 'super_admin'
  AND p.code IN ('school.profile:read', 'branch:read', 'media.public:view');

CREATE TABLE email_verification_token (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_user(id),
    email text NOT NULL,
    token_hash text NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz
);
CREATE INDEX email_verification_user_idx ON email_verification_token(user_id);

CREATE OR REPLACE FUNCTION school_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_actor    uuid := app_actor();
    v_user     app_user%ROWTYPE;
    v_default  branch%ROWTYPE;
    v_slug     text;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'schools are never deleted; reject or suspend them'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF TG_OP = 'INSERT' THEN
        SELECT * INTO v_user FROM app_user WHERE id = v_actor;
        IF v_user.kind IS DISTINCT FROM 'person' OR NOT v_user.is_active OR v_user.verified_at IS NULL THEN
            RAISE EXCEPTION 'only verified, active people can register a school'
                USING ERRCODE = 'insufficient_privilege';
        END IF;
        NEW.created_by        := v_actor;
        NEW.status            := 'draft';
        NEW.status_reason     := NULL;
        NEW.status_changed_at := now();
        NEW.created_at        := now();
        NEW.logo_asset_id     := NULL;
        NEW.cover_asset_id    := NULL;
        RETURN NEW;
    END IF;
    IF NEW.created_by <> OLD.created_by OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'created_by and created_at never change' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF (NEW.display_name, NEW.legal_name, NEW.registration_no, NEW.description,
        NEW.contact_email, NEW.contact_phone, NEW.logo_asset_id, NEW.cover_asset_id)
       IS DISTINCT FROM
       (OLD.display_name, OLD.legal_name, OLD.registration_no, OLD.description,
        OLD.contact_email, OLD.contact_phone, OLD.logo_asset_id, OLD.cover_asset_id) THEN
        IF OLD.status = 'pending_review' THEN
            RAISE EXCEPTION 'the profile is locked while it is under review' USING ERRCODE = 'check_violation';
        END IF;
        IF OLD.status IN ('approved', 'suspended')
           AND (NEW.display_name, NEW.legal_name, NEW.registration_no)
               IS DISTINCT FROM (OLD.display_name, OLD.legal_name, OLD.registration_no) THEN
            RAISE EXCEPTION 'reviewed identity fields cannot change after approval; contact support'
                USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.logo_asset_id IS DISTINCT FROM OLD.logo_asset_id AND NEW.logo_asset_id IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM media_asset
                           WHERE id = NEW.logo_asset_id AND purpose = 'school_logo' AND status = 'ready') THEN
            RAISE EXCEPTION 'the logo must be a ready school_logo image' USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.cover_asset_id IS DISTINCT FROM OLD.cover_asset_id AND NEW.cover_asset_id IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM media_asset
                           WHERE id = NEW.cover_asset_id AND purpose = 'school_cover' AND status = 'ready') THEN
            RAISE EXCEPTION 'the cover must be a ready school_cover image' USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        NEW.status_changed_at := now();
        IF OLD.status IN ('draft', 'changes_requested') AND NEW.status = 'pending_review' THEN
            SELECT * INTO v_default FROM branch WHERE school_id = NEW.id AND is_default;
            SELECT w.slug INTO v_slug FROM workspace w
            WHERE w.branch_id = v_default.id AND w.retired_at IS NULL;
            IF NEW.logo_asset_id IS NULL OR NEW.cover_asset_id IS NULL
               OR (SELECT count(*) FROM media_asset
                   WHERE id IN (NEW.logo_asset_id, NEW.cover_asset_id) AND status = 'ready') <> 2
               OR v_default.id IS NULL OR v_default.address_line IS NULL
               OR v_default.province IS NULL OR v_default.latitude IS NULL
               OR v_slug IS NULL THEN
                RAISE EXCEPTION 'incomplete: needs a logo, a cover, a default branch with address and location, and a workspace'
                    USING ERRCODE = 'check_violation';
            END IF;
            NEW.status_reason := NULL;
            INSERT INTO school_review (school_id, submitted_by, snapshot)
            VALUES (NEW.id, v_actor, jsonb_build_object(
                'display_name',    NEW.display_name,
                'legal_name',      NEW.legal_name,
                'registration_no', NEW.registration_no,
                'description', NEW.description,
                'contact_email', NEW.contact_email,
                'contact_phone', NEW.contact_phone,
                'logo_asset_id',   NEW.logo_asset_id,
                'cover_asset_id',  NEW.cover_asset_id,
                'default_branch',  to_jsonb(v_default),
                'workspace',       v_slug));
        ELSIF OLD.status = 'pending_review' AND NEW.status IN ('approved', 'changes_requested', 'rejected') THEN
            IF v_actor = NEW.created_by
               OR EXISTS (SELECT 1 FROM user_role WHERE user_id = v_actor AND school_id = NEW.id) THEN
                RAISE EXCEPTION 'reviewers cannot decide on a school they belong to'
                    USING ERRCODE = 'insufficient_privilege';
            END IF;
            IF NEW.status <> 'approved' AND COALESCE(btrim(NEW.status_reason), '') = '' THEN
                RAISE EXCEPTION 'a reason is required' USING ERRCODE = 'check_violation';
            END IF;
            UPDATE school_review
            SET decided_by = v_actor, decided_at = now(), decision = NEW.status, notes = NEW.status_reason
            WHERE school_id = NEW.id AND decided_at IS NULL;
        ELSIF (OLD.status = 'approved' AND NEW.status = 'suspended')
           OR (OLD.status = 'suspended' AND NEW.status = 'approved') THEN
            IF NEW.status = 'suspended' AND COALESCE(btrim(NEW.status_reason), '') = '' THEN
                RAISE EXCEPTION 'a reason is required' USING ERRCODE = 'check_violation';
            END IF;
        ELSE
            RAISE EXCEPTION 'a school cannot move from % to %', OLD.status, NEW.status
                USING ERRCODE = 'check_violation';
        END IF;
        PERFORM audit(NEW.id, 'school', NEW.id, NEW.status,
                      jsonb_build_object('from', OLD.status, 'reason', NEW.status_reason));
    ELSIF NEW.status_reason IS DISTINCT FROM OLD.status_reason THEN
        RAISE EXCEPTION 'status_reason changes only with the status' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

