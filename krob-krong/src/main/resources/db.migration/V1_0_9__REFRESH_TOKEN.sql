CREATE TABLE refresh_token (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid NOT NULL REFERENCES app_user (id),
    school_id    uuid REFERENCES school (id),
    token_hash   text NOT NULL,
    family_id    text NOT NULL,
    replaced_by  text,
    user_agent   text,
    ip           text,
    expires_at   timestamptz NOT NULL,
    revoked_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX refresh_token_hash_idx ON refresh_token (token_hash);
CREATE INDEX refresh_token_family_idx ON refresh_token (family_id);
