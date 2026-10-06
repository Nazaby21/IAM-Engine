-- role_permission guard
CREATE FUNCTION role_permission_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_kind text;
BEGIN
    SELECT kind INTO v_kind FROM role WHERE id = NEW.role_id;
    IF NOT (
           (v_kind = 'anyone'        AND NEW.scope = 'public')
        OR (v_kind = 'authenticated' AND NEW.scope IN ('public', 'own'))
        OR (v_kind = 'platform'      AND NEW.scope = 'platform')
        OR (v_kind = 'school'        AND NEW.scope IN ('tenant', 'own'))
        OR (v_kind = 'support'       AND NEW.scope = 'tenant'
                                     AND NEW.permission_code ~ ':(read|view)$')
    ) THEN
        RAISE EXCEPTION 'scope % is not allowed for % role permission %',
            NEW.scope, v_kind, NEW.permission_code
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER role_permission_guard_trg
    BEFORE INSERT OR UPDATE ON role_permission
    FOR EACH ROW EXECUTE FUNCTION role_permission_guard();

-- user_role guard
CREATE FUNCTION user_role_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    r         role%ROWTYPE;
    v_user    app_user%ROWTYPE;
    v_school  school%ROWTYPE;
    v_actor   uuid;
    v_ending  boolean := false;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'role grants are never deleted; revoke them'
            USING ERRCODE = 'insufficient_privilege';
    END IF;

    SELECT * INTO r FROM role WHERE id = NEW.role_id;

    IF TG_OP = 'INSERT' THEN
        SELECT * INTO v_user FROM app_user WHERE id = NEW.user_id;
        IF v_user.kind <> 'person' THEN
            RAISE EXCEPTION 'service accounts cannot hold roles' USING ERRCODE = 'check_violation';
        END IF;
        IF r.kind NOT IN ('platform', 'school') THEN
            RAISE EXCEPTION 'role % is never assigned', r.code USING ERRCODE = 'check_violation';
        END IF;
        IF (r.kind = 'platform') <> (NEW.school_id IS NULL) THEN
            RAISE EXCEPTION 'role % must be assigned at % level', r.code, r.kind
                USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.branch_id IS NOT NULL AND NOT r.allows_branch THEN
            RAISE EXCEPTION 'role % cannot be limited to a branch', r.code
                USING ERRCODE = 'check_violation';
        END IF;
        IF r.kind = 'platform' AND v_user.mfa_enabled_at IS NULL THEN
            RAISE EXCEPTION 'platform roles require MFA' USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.school_id IS NOT NULL THEN
            SELECT * INTO v_school FROM school WHERE id = NEW.school_id;
            IF v_school.status IN ('suspended', 'rejected') THEN
                RAISE EXCEPTION 'school is %', v_school.status USING ERRCODE = 'check_violation';
            END IF;
            IF v_school.status <> 'approved' AND NOT r.pre_approval THEN
                RAISE EXCEPTION '% can only be invited after the school is approved', r.code
                    USING ERRCODE = 'check_violation';
            END IF;
        END IF;

        NEW.granted_at := now();
        NEW.revoked_at := NULL;
        NEW.revoked_by := NULL;

        IF NEW.source = 'bootstrap' THEN
            IF r.kind <> 'platform' OR EXISTS (
                SELECT 1 FROM user_role ur JOIN role rr ON rr.id = ur.role_id
                WHERE rr.kind = 'platform' AND ur.accepted_at IS NOT NULL
                  AND tstzrange(ur.valid_from, ur.valid_to) @> now()) THEN
                RAISE EXCEPTION 'bootstrap is only for the first platform admin'
                    USING ERRCODE = 'insufficient_privilege';
            END IF;
            NEW.granted_by  := NULL;
            NEW.accepted_at := now();
            PERFORM audit(NULL, 'user_role', NEW.id, 'bootstrapped', NULL);
            RETURN NEW;
        END IF;

        v_actor := app_actor();

        IF NEW.source = 'onboarding' THEN
            IF r.code <> 'school_owner'
               OR v_actor <> NEW.user_id
               OR v_school.created_by <> NEW.user_id
               OR EXISTS (SELECT 1 FROM user_role o
                          WHERE o.school_id = NEW.school_id AND o.role_id = NEW.role_id) THEN
                RAISE EXCEPTION 'onboarding grants are created only for the school''s creator'
                    USING ERRCODE = 'insufficient_privilege';
            END IF;
            NEW.granted_by  := NULL;
            NEW.accepted_at := now();
            PERFORM audit(NEW.school_id, 'user_role', NEW.id, 'granted',
                          jsonb_build_object('role', r.code, 'user_id', NEW.user_id));
            RETURN NEW;
        END IF;

        NEW.granted_by := v_actor;
        IF v_actor = NEW.user_id THEN
            RAISE EXCEPTION 'nobody can grant themselves a role' USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.accepted_at IS NOT NULL THEN
            RAISE EXCEPTION 'only the invited person can accept a role'
                USING ERRCODE = 'insufficient_privilege';
        END IF;
        PERFORM audit(NEW.school_id, 'user_role', NEW.id, 'invited',
                      jsonb_build_object('role', r.code, 'user_id', NEW.user_id, 'branch_id', NEW.branch_id));
        RETURN NEW;
    END IF;

    v_actor := app_actor();

    IF NEW.user_id <> OLD.user_id OR NEW.role_id <> OLD.role_id
       OR NEW.school_id IS DISTINCT FROM OLD.school_id
       OR NEW.branch_id IS DISTINCT FROM OLD.branch_id
       OR NEW.source <> OLD.source
       OR NEW.valid_from <> OLD.valid_from
       OR NEW.granted_by IS DISTINCT FROM OLD.granted_by
       OR NEW.granted_at <> OLD.granted_at THEN
        RAISE EXCEPTION 'role grants are immutable; revoke and grant a new one'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF OLD.revoked_at IS NOT NULL THEN
        RAISE EXCEPTION 'grant % is already revoked', OLD.id USING ERRCODE = 'check_violation';
    END IF;

    IF NEW.accepted_at IS DISTINCT FROM OLD.accepted_at THEN
        IF OLD.accepted_at IS NOT NULL OR NEW.accepted_at IS NULL OR v_actor <> NEW.user_id THEN
            RAISE EXCEPTION 'only the invited person can accept, once'
                USING ERRCODE = 'insufficient_privilege';
        END IF;
        NEW.accepted_at := now();
        PERFORM audit(NEW.school_id, 'user_role', NEW.id, 'accepted', NULL);
    END IF;

    IF NEW.revoked_at IS NOT NULL THEN
        NEW.revoked_at := now();
        NEW.revoked_by := v_actor;
        NEW.valid_to   := GREATEST(OLD.valid_from, LEAST(COALESCE(OLD.valid_to, now()), now()));
        v_ending := true;
    ELSIF NEW.revoked_by IS NOT NULL THEN
        RAISE EXCEPTION 'set revoked_at to revoke' USING ERRCODE = 'check_violation';
    ELSIF NEW.valid_to IS DISTINCT FROM OLD.valid_to THEN
        IF NEW.valid_to IS NULL
           OR (OLD.valid_to IS NOT NULL AND NEW.valid_to > OLD.valid_to)
           OR NEW.valid_to < OLD.valid_from THEN
            RAISE EXCEPTION 'grants can be shortened, never extended'
                USING ERRCODE = 'check_violation';
        END IF;
        v_ending := true;
    END IF;

    IF v_ending THEN
        IF r.code = 'school_owner' AND OLD.accepted_at IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM user_role o
            WHERE o.school_id = NEW.school_id AND o.role_id = NEW.role_id AND o.id <> NEW.id
              AND o.accepted_at IS NOT NULL AND o.revoked_at IS NULL
              AND tstzrange(o.valid_from, o.valid_to) @> now()) THEN
            RAISE EXCEPTION 'a school must keep at least one owner; add another owner first'
                USING ERRCODE = 'check_violation';
        END IF;
        PERFORM audit(NEW.school_id, 'user_role', NEW.id, 'revoked',
                      jsonb_build_object('reason', NEW.revoke_reason, 'valid_to', NEW.valid_to));
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER user_role_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON user_role
    FOR EACH ROW EXECUTE FUNCTION user_role_guard();

-- support_access guard
CREATE FUNCTION support_access_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_actor uuid := app_actor();
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'support sessions are never deleted' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF TG_OP = 'INSERT' THEN
        NEW.user_id    := v_actor;
        NEW.started_at := now();
        NEW.ended_at   := NULL;
        PERFORM audit(NEW.school_id, 'support_access', NEW.id, 'started',
                      jsonb_build_object('reason', NEW.reason, 'expires_at', NEW.expires_at));
        RETURN NEW;
    END IF;
    IF (NEW.user_id, NEW.school_id, NEW.reason, NEW.started_at, NEW.expires_at)
       IS DISTINCT FROM (OLD.user_id, OLD.school_id, OLD.reason, OLD.started_at, OLD.expires_at) THEN
        RAISE EXCEPTION 'support sessions can only be ended' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF OLD.ended_at IS NOT NULL THEN
        RAISE EXCEPTION 'session already ended' USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.ended_at IS NOT NULL THEN
        NEW.ended_at := now();
        PERFORM audit(NEW.school_id, 'support_access', NEW.id, 'ended', NULL);
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER support_access_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON support_access
    FOR EACH ROW EXECUTE FUNCTION support_access_guard();

-- school guard
CREATE FUNCTION school_guard() RETURNS trigger
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

CREATE TRIGGER school_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON school
    FOR EACH ROW EXECUTE FUNCTION school_guard();

CREATE FUNCTION school_onboard() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO user_role (user_id, role_id, school_id, source)
    SELECT NEW.created_by, r.id, NEW.id, 'onboarding'
    FROM role r WHERE r.code = 'school_owner';
    PERFORM audit(NEW.id, 'school', NEW.id, 'registered', NULL);
    RETURN NULL;
END;
$$;

CREATE TRIGGER school_onboard_trg
    AFTER INSERT ON school
    FOR EACH ROW EXECUTE FUNCTION school_onboard();

-- branch guard
CREATE FUNCTION branch_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_actor          uuid := app_actor();
    v_school_status  text;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'branches are never deleted; close them' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT status INTO v_school_status FROM school WHERE id = NEW.school_id;
    IF TG_OP = 'INSERT' THEN
        IF v_school_status NOT IN ('draft', 'changes_requested', 'approved') THEN
            RAISE EXCEPTION 'cannot add branches while the school is %', v_school_status
                USING ERRCODE = 'check_violation';
        END IF;
        NEW.created_by := v_actor;
        NEW.created_at := now();
        NEW.status     := 'draft';
        NEW.opened_at  := NULL;
        RETURN NEW;
    END IF;
    IF NEW.school_id <> OLD.school_id OR NEW.created_by <> OLD.created_by
       OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'school_id, created_by and created_at never change'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF (NEW.name, NEW.address_line, NEW.district, NEW.province, NEW.country_code,
        NEW.latitude, NEW.longitude, NEW.timezone)
       IS DISTINCT FROM
       (OLD.name, OLD.address_line, OLD.district, OLD.province, OLD.country_code,
        OLD.latitude, OLD.longitude, OLD.timezone) THEN
        IF NEW.is_default AND v_school_status = 'pending_review' THEN
            RAISE EXCEPTION 'the default branch is locked while the school is under review'
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    IF NEW.is_default IS DISTINCT FROM OLD.is_default THEN
        IF v_school_status = 'pending_review' OR (NEW.is_default AND NEW.status = 'closed') THEN
            RAISE EXCEPTION 'cannot change the default branch now' USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF NEW.status = 'active' THEN
            IF v_school_status <> 'approved' THEN
                RAISE EXCEPTION 'the school must be approved before any branch opens'
                    USING ERRCODE = 'insufficient_privilege';
            END IF;
            IF NEW.address_line IS NULL OR NEW.latitude IS NULL
               OR NOT EXISTS (SELECT 1 FROM workspace w WHERE w.branch_id = NEW.id AND w.retired_at IS NULL) THEN
                RAISE EXCEPTION 'a branch needs an address, a location and a workspace before it opens'
                    USING ERRCODE = 'check_violation';
            END IF;
            NEW.opened_at := COALESCE(OLD.opened_at, now());
        ELSIF NEW.status = 'closed' THEN
            IF NEW.is_default THEN
                RAISE EXCEPTION 'make another branch the default before closing this one'
                    USING ERRCODE = 'check_violation';
            END IF;
        ELSE
            RAISE EXCEPTION 'an opened branch cannot return to draft' USING ERRCODE = 'check_violation';
        END IF;
        PERFORM audit(NEW.school_id, 'branch', NEW.id, NEW.status, jsonb_build_object('from', OLD.status));
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER branch_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON branch
    FOR EACH ROW EXECUTE FUNCTION branch_guard();

-- workspace guard
CREATE FUNCTION workspace_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_actor          uuid := app_actor();
    v_school_status  text;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'workspaces are never deleted; retire them' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT status INTO v_school_status FROM school WHERE id = NEW.school_id;
    IF TG_OP = 'INSERT' THEN
        NEW.slug := lower(btrim(NEW.slug));
        IF EXISTS (SELECT 1 FROM reserved_slug WHERE slug = NEW.slug) THEN
            RAISE EXCEPTION 'workspace name % is reserved', NEW.slug USING ERRCODE = 'check_violation';
        END IF;
        IF v_school_status NOT IN ('draft', 'changes_requested', 'approved') THEN
            RAISE EXCEPTION 'cannot change workspaces while the school is %', v_school_status
                USING ERRCODE = 'check_violation';
        END IF;
        NEW.created_by := v_actor;
        NEW.created_at := now();
        NEW.retired_at := NULL;
        PERFORM audit(NEW.school_id, 'workspace', NEW.id, 'created', jsonb_build_object('slug', NEW.slug));
        RETURN NEW;
    END IF;
    IF (NEW.school_id, NEW.branch_id, NEW.slug, NEW.created_by, NEW.created_at)
       IS DISTINCT FROM (OLD.school_id, OLD.branch_id, OLD.slug, OLD.created_by, OLD.created_at) THEN
        RAISE EXCEPTION 'a workspace name never changes; retire it and create a new one'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF OLD.retired_at IS NOT NULL THEN
        RAISE EXCEPTION 'workspace % is already retired', OLD.slug USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.retired_at IS NOT NULL THEN
        IF v_school_status = 'pending_review' THEN
            RAISE EXCEPTION 'workspaces are locked while the school is under review'
                USING ERRCODE = 'check_violation';
        END IF;
        NEW.retired_at := now();
        PERFORM audit(NEW.school_id, 'workspace', NEW.id, 'retired', jsonb_build_object('slug', NEW.slug));
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER workspace_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON workspace
    FOR EACH ROW EXECUTE FUNCTION workspace_guard();

-- media_asset guard
CREATE FUNCTION media_asset_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_actor       uuid := app_actor();
    v_is_service  boolean;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'media rows are never deleted; set status deleted'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    v_is_service := EXISTS (SELECT 1 FROM app_user WHERE id = v_actor AND is_active AND kind = 'service');
    IF TG_OP = 'INSERT' THEN
        IF NEW.school_id IS NULL
           AND NOT EXISTS (SELECT 1 FROM app_user WHERE id = v_actor AND is_active AND kind = 'person') THEN
            RAISE EXCEPTION 'personal uploads are for active people' USING ERRCODE = 'insufficient_privilege';
        END IF;
        NEW.uploaded_by := v_actor;
        NEW.status      := 'pending_upload';
        NEW.storage_key := COALESCE('schools/' || NEW.school_id::text, 'users/' || v_actor::text)
                           || '/' || NEW.id::text;
        NEW.sha256      := NULL;
        NEW.width       := NULL;
        NEW.height      := NULL;
        NEW.duration_ms := NULL;
        NEW.created_at  := now();
        NEW.ready_at    := NULL;
        NEW.deleted_at  := NULL;
        RETURN NEW;
    END IF;
    IF (NEW.school_id, NEW.branch_id, NEW.uploaded_by, NEW.kind, NEW.purpose, NEW.storage_key, NEW.created_at)
       IS DISTINCT FROM
       (OLD.school_id, OLD.branch_id, OLD.uploaded_by, OLD.kind, OLD.purpose, OLD.storage_key, OLD.created_at) THEN
        RAISE EXCEPTION 'owner, location, kind and purpose of a file never change'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF (NEW.mime_type, NEW.byte_size, NEW.sha256, NEW.width, NEW.height, NEW.duration_ms)
       IS DISTINCT FROM
       (OLD.mime_type, OLD.byte_size, OLD.sha256, OLD.width, OLD.height, OLD.duration_ms)
       AND NOT v_is_service THEN
        RAISE EXCEPTION 'only the media pipeline records file metadata'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF OLD.status = 'deleted' THEN
            RAISE EXCEPTION 'deleted media stays deleted' USING ERRCODE = 'check_violation';
        END IF;
        IF OLD.status = 'pending_upload' AND NEW.status = 'processing' THEN
            IF NOT (v_is_service OR v_actor = NEW.uploaded_by) THEN
                RAISE EXCEPTION 'only the uploader or the pipeline can start processing'
                    USING ERRCODE = 'insufficient_privilege';
            END IF;
        ELSIF OLD.status = 'processing' AND NEW.status IN ('ready', 'failed', 'quarantined') THEN
            IF NOT v_is_service THEN
                RAISE EXCEPTION 'only the media pipeline finishes processing'
                    USING ERRCODE = 'insufficient_privilege';
            END IF;
            IF NEW.status = 'ready' THEN NEW.ready_at := now(); END IF;
        ELSIF OLD.status = 'ready' AND NEW.status = 'quarantined' THEN
            NULL;
        ELSIF NEW.status = 'deleted' THEN
            IF EXISTS (SELECT 1 FROM school WHERE logo_asset_id = NEW.id OR cover_asset_id = NEW.id)
               OR EXISTS (SELECT 1 FROM app_user WHERE avatar_asset_id = NEW.id) THEN
                RAISE EXCEPTION 'this file is in use as a logo, cover or avatar; replace it first'
                    USING ERRCODE = 'check_violation';
            END IF;
            NEW.deleted_at := now();
        ELSE
            RAISE EXCEPTION 'media cannot move from % to %', OLD.status, NEW.status
                USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.status IN ('quarantined', 'deleted') THEN
            PERFORM audit(NEW.school_id, 'media_asset', NEW.id, NEW.status, NULL);
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER media_asset_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON media_asset
    FOR EACH ROW EXECUTE FUNCTION media_asset_guard();

-- media_rendition guard
CREATE FUNCTION media_rendition_guard() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE id = app_actor() AND is_active AND kind = 'service') THEN
        RAISE EXCEPTION 'only the media pipeline writes renditions' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF TG_OP = 'INSERT' THEN
        NEW.storage_key := (SELECT storage_key FROM media_asset WHERE id = NEW.asset_id) || '/' || NEW.name;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER media_rendition_guard_trg
    BEFORE INSERT OR UPDATE OR DELETE ON media_rendition
    FOR EACH ROW EXECUTE FUNCTION media_rendition_guard();
