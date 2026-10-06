CREATE TABLE support_access (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES app_user (id),
    school_id   uuid NOT NULL REFERENCES school (id),
    reason      text NOT NULL CHECK (length(btrim(reason)) >= 15),
    started_at  timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL,
    ended_at    timestamptz,
    CHECK (expires_at > started_at AND expires_at <= started_at + interval '4 hours')
);
CREATE INDEX support_access_user_idx ON support_access (user_id, school_id);

CREATE TABLE audit_event (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    at         timestamptz NOT NULL DEFAULT now(),
    actor_id   uuid,
    school_id  uuid,
    entity     text NOT NULL,
    entity_id  uuid NOT NULL,
    action     text NOT NULL,
    detail     jsonb
);
CREATE INDEX audit_event_school_idx ON audit_event (school_id, at);

CREATE FUNCTION audit(p_school uuid, p_entity text, p_id uuid, p_action text, p_detail jsonb DEFAULT NULL)
RETURNS void
LANGUAGE sql AS $$
    INSERT INTO audit_event (actor_id, school_id, entity, entity_id, action, detail)
    VALUES (app_actor_or_null(), p_school, p_entity, p_id, p_action, p_detail)
$$;

CREATE TABLE access_log (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    at               timestamptz NOT NULL DEFAULT now(),
    user_id          uuid,
    permission_code  text NOT NULL,
    school_id        uuid,
    branch_id        uuid,
    target_id        uuid,
    allowed          boolean NOT NULL
);
CREATE INDEX access_log_school_idx ON access_log (school_id, at);
