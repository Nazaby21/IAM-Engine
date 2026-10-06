CREATE TABLE refresh_token (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid NOT NULL REFERENCES app_user (id),
    family_id    uuid NOT NULL,
    token_hash   text NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    replaced_by  uuid,
    user_agent   text,
    ip           text,
    expires_at   timestamptz NOT NULL,
    revoked_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now()
);
-- Serves the per-request active-session check on access tokens.
CREATE INDEX refresh_token_active_family_idx ON refresh_token (family_id) WHERE revoked_at IS NULL;
CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);
