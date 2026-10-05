-- ============================================================================
--  Krob Krong RBAC — multi-tenant, module-matrix, hierarchical roles
--  V1_0_0: core schema
--
--  ID convention: application-generated `UUID` strings
--  seeded and referenced by natural key (module, permission) use their code as PK.
-- ============================================================================

-- ----------------------------------------------------------------------------
--  Tenants (organizations / workspaces)
-- ----------------------------------------------------------------------------
CREATE TABLE tenant (
    id          VARCHAR(64)  PRIMARY KEY,
    slug        VARCHAR(64)  NOT NULL,
    name        VARCHAR(160) NOT NULL,
    status      VARCHAR(24)  NOT NULL DEFAULT 'active',   -- active | suspended
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_tenant_slug ON tenant (lower(slug));

-- ----------------------------------------------------------------------------
--  Users (global identity — a user may belong to many tenants)
-- ----------------------------------------------------------------------------
CREATE TABLE app_user (
    id                VARCHAR(64)  PRIMARY KEY,
    email             VARCHAR(320) NOT NULL,
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    password_hash     VARCHAR(100) NOT NULL,               -- bcrypt
    display_name      VARCHAR(160) NOT NULL,
    avatar_url        VARCHAR(512),
    status            VARCHAR(24)  NOT NULL DEFAULT 'active', -- active | suspended | invited
    is_platform_admin BOOLEAN      NOT NULL DEFAULT FALSE,  -- cross-tenant super operator
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));

-- ----------------------------------------------------------------------------
--  Modules — the operational "layers" the admin configures roles against
-- ----------------------------------------------------------------------------
CREATE TABLE module (
    code        VARCHAR(48)  PRIMARY KEY,                  -- e.g. roles, members, chat
    name        VARCHAR(96)  NOT NULL,
    description VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
--  Permission catalog — one row per (module, action)
--  code is the natural key in `module:action` form (framework scope appended
--  at token-mint time as `module:action:*`).
-- ----------------------------------------------------------------------------
CREATE TABLE permission (
    code        VARCHAR(96)  PRIMARY KEY,                  -- e.g. roles:update
    module_code VARCHAR(48)  NOT NULL REFERENCES module (code) ON DELETE CASCADE,
    action      VARCHAR(48)  NOT NULL,                     -- read|create|update|delete|invite|assign|...
    description VARCHAR(255),
    CONSTRAINT ux_permission_module_action UNIQUE (module_code, action)
);
CREATE INDEX ix_permission_module ON permission (module_code);

-- ----------------------------------------------------------------------------
--  Roles — system templates (tenant_id NULL) or tenant-scoped, with hierarchy
-- ----------------------------------------------------------------------------
CREATE TABLE role (
    id             VARCHAR(64)  PRIMARY KEY,
    tenant_id      VARCHAR(64)  REFERENCES tenant (id) ON DELETE CASCADE,  -- NULL = system template
    role_key       VARCHAR(64)  NOT NULL,                 -- slug, e.g. super_admin
    name           VARCHAR(96)  NOT NULL,
    description    VARCHAR(255),
    rank           INT          NOT NULL DEFAULT 0,        -- higher = more powerful (privilege ceiling)
    parent_role_id VARCHAR(64)  REFERENCES role (id) ON DELETE SET NULL,   -- inheritance
    is_system      BOOLEAN      NOT NULL DEFAULT FALSE,    -- template / not deletable
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,    -- auto-assigned to new members
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Unique role_key per tenant; system templates (tenant_id NULL) unique on their own.
CREATE UNIQUE INDEX ux_role_tenant_key ON role (tenant_id, lower(role_key)) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX ux_role_system_key ON role (lower(role_key)) WHERE tenant_id IS NULL;
CREATE INDEX ix_role_tenant ON role (tenant_id);
CREATE INDEX ix_role_parent ON role (parent_role_id);

-- ----------------------------------------------------------------------------
--  Role → Permission (the matrix)
-- ----------------------------------------------------------------------------
CREATE TABLE role_permission (
    role_id         VARCHAR(64) NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    permission_code VARCHAR(96) NOT NULL REFERENCES permission (code) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_code)
);
CREATE INDEX ix_role_permission_perm ON role_permission (permission_code);

-- ----------------------------------------------------------------------------
--  Membership — user within a tenant
-- ----------------------------------------------------------------------------
CREATE TABLE membership (
    id         VARCHAR(64) PRIMARY KEY,
    user_id    VARCHAR(64) NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    tenant_id  VARCHAR(64) NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    status     VARCHAR(24) NOT NULL DEFAULT 'active',      -- active | invited | suspended
    is_owner   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_membership_user_tenant UNIQUE (user_id, tenant_id)
);
CREATE INDEX ix_membership_tenant ON membership (tenant_id);
CREATE INDEX ix_membership_user ON membership (user_id);

-- ----------------------------------------------------------------------------
--  Membership → Role (a member may hold several roles)
-- ----------------------------------------------------------------------------
CREATE TABLE membership_role (
    membership_id VARCHAR(64) NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    role_id       VARCHAR(64) NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    PRIMARY KEY (membership_id, role_id)
);
CREATE INDEX ix_membership_role_role ON membership_role (role_id);

-- ----------------------------------------------------------------------------
--  Refresh tokens — rotation with reuse detection (token family)
-- ----------------------------------------------------------------------------
CREATE TABLE refresh_token (
    id          VARCHAR(64)  PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    tenant_id   VARCHAR(64)  REFERENCES tenant (id) ON DELETE SET NULL,  -- active tenant at issue time
    token_hash  VARCHAR(128) NOT NULL,                     -- sha-256 hex of opaque token
    family_id   VARCHAR(64)  NOT NULL,
    replaced_by VARCHAR(64),
    user_agent  VARCHAR(255),
    ip          VARCHAR(64),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_token (token_hash);
CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);
CREATE INDEX ix_refresh_token_family ON refresh_token (family_id);

-- ----------------------------------------------------------------------------
--  Audit log — administrative actions
-- ----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id            VARCHAR(64) PRIMARY KEY,
    tenant_id     VARCHAR(64),
    actor_user_id VARCHAR(64),
    action        VARCHAR(96) NOT NULL,                    -- role.create | role.permissions.update | member.role.assign | ...
    target_type   VARCHAR(48),
    target_id     VARCHAR(64),
    detail        JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_tenant_time ON audit_log (tenant_id, created_at DESC);
