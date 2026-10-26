CREATE TABLE mfa_totp (
    user_id            uuid PRIMARY KEY REFERENCES app_user (id),
    secret_ciphertext  text NOT NULL,
    created_at         timestamptz NOT NULL DEFAULT now(),
    activated_at       timestamptz,
    last_used_step     bigint,
    failed_attempts    integer NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    locked_until       timestamptz
);

ALTER TABLE refresh_token ADD COLUMN mfa_verified boolean NOT NULL DEFAULT false;
