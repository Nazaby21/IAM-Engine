CREATE TABLE app_user (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kind             text NOT NULL DEFAULT 'person' CHECK (kind IN ('person', 'service')),
    email            text,
    phone            text,
    display_name     text NOT NULL,
    avatar_asset_id  uuid,
    verified_at      timestamptz,
    mfa_enabled_at   timestamptz,
    is_active        boolean NOT NULL DEFAULT true,
    created_at       timestamptz NOT NULL DEFAULT now(),
    CHECK (kind = 'service' OR email IS NOT NULL OR phone IS NOT NULL)
);
CREATE UNIQUE INDEX app_user_email_uq ON app_user (lower(email));
CREATE UNIQUE INDEX app_user_phone_uq ON app_user (phone);
