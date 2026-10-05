CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE FUNCTION app_actor_or_null() RETURNS uuid
LANGUAGE sql STABLE AS $$
    SELECT NULLIF(current_setting('app.actor_id', true), '')::uuid
$$;

CREATE FUNCTION app_actor() RETURNS uuid
LANGUAGE plpgsql STABLE AS $$
DECLARE
    v uuid := app_actor_or_null();
BEGIN
    IF v IS NULL THEN
        RAISE EXCEPTION 'app.actor_id is not set for this transaction'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN v;
END;
$$;
