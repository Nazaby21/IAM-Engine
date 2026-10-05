CREATE TABLE school (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by         uuid NOT NULL REFERENCES app_user (id),
    display_name       text NOT NULL CHECK (length(btrim(display_name)) BETWEEN 2 AND 120),
    legal_name         text,
    registration_no    text,
    description        text,
    contact_email      text,
    contact_phone      text,
    logo_asset_id      uuid,
    cover_asset_id     uuid,
    status             text NOT NULL DEFAULT 'draft'
                       CHECK (status IN ('draft', 'pending_review', 'changes_requested',
                                         'approved', 'rejected', 'suspended')),
    status_reason      text,
    status_changed_at  timestamptz NOT NULL DEFAULT now(),
    created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE school_review (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     uuid NOT NULL REFERENCES school (id),
    submitted_by  uuid NOT NULL REFERENCES app_user (id),
    submitted_at  timestamptz NOT NULL DEFAULT now(),
    snapshot      jsonb NOT NULL,
    decided_by    uuid REFERENCES app_user (id),
    decided_at    timestamptz,
    decision      text CHECK (decision IN ('approved', 'changes_requested', 'rejected')),
    notes         text,
    CHECK ((decided_at IS NULL) = (decision IS NULL)),
    CHECK ((decided_at IS NULL) = (decided_by IS NULL))
);
CREATE UNIQUE INDEX school_review_one_open ON school_review (school_id) WHERE decided_at IS NULL;
