CREATE TYPE access_scope AS ENUM ('public', 'own', 'tenant', 'platform');

CREATE TABLE permission (
    code          text PRIMARY KEY CHECK (code ~ '^[a-z_.]+:[a-z_]+$'),
    description   text NOT NULL,
    is_sensitive  boolean NOT NULL DEFAULT false
);

CREATE TABLE role (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code           text NOT NULL UNIQUE,
    name           text NOT NULL,
    kind           text NOT NULL CHECK (kind IN ('anyone', 'authenticated', 'platform', 'school', 'support')),
    allows_branch  boolean NOT NULL DEFAULT false,
    pre_approval   boolean NOT NULL DEFAULT false,
    CHECK (kind = 'school' OR NOT (allows_branch OR pre_approval))
);

CREATE TABLE role_permission (
    role_id          uuid NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    permission_code  text NOT NULL REFERENCES permission (code),
    scope            access_scope NOT NULL,
    PRIMARY KEY (role_id, permission_code, scope)
);

CREATE TABLE role_grant_rule (
    granter_role_id  uuid NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    grantee_role_id  uuid NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    PRIMARY KEY (granter_role_id, grantee_role_id)
);

CREATE TABLE user_role (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES app_user (id),
    role_id        uuid NOT NULL REFERENCES role (id),
    school_id      uuid REFERENCES school (id),
    branch_id      uuid,
    source         text NOT NULL DEFAULT 'invite'
                   CHECK (source IN ('invite', 'onboarding', 'bootstrap')),
    valid_from     timestamptz NOT NULL DEFAULT now(),
    valid_to       timestamptz,
    granted_by     uuid REFERENCES app_user (id),
    granted_at     timestamptz NOT NULL DEFAULT now(),
    accepted_at    timestamptz,
    revoked_by     uuid REFERENCES app_user (id),
    revoked_at     timestamptz,
    revoke_reason  text,
    FOREIGN KEY (school_id, branch_id) REFERENCES branch (school_id, id),
    CHECK (branch_id IS NULL OR school_id IS NOT NULL),
    CHECK ((source = 'invite') = (granted_by IS NOT NULL)),
    CHECK (granted_by IS DISTINCT FROM user_id),
    CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CHECK ((revoked_at IS NULL) = (revoked_by IS NULL)),
    CHECK (revoked_at IS NULL OR valid_to IS NOT NULL),
    EXCLUDE USING gist (
        user_id WITH =,
        role_id WITH =,
        (COALESCE(school_id, '00000000-0000-0000-0000-000000000000'::uuid)) WITH =,
        (COALESCE(branch_id, '00000000-0000-0000-0000-000000000000'::uuid)) WITH =,
        tstzrange(valid_from, valid_to) WITH &&)
);
CREATE INDEX user_role_user_idx ON user_role (user_id);
CREATE INDEX user_role_school_idx ON user_role (school_id, role_id);
