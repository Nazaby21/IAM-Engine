CREATE TABLE branch (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     uuid NOT NULL REFERENCES school (id),
    name          text NOT NULL,
    is_default    boolean NOT NULL DEFAULT false,
    status        text NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'closed')),
    address_line  text,
    district      text,
    province      text,
    country_code  char(2) NOT NULL DEFAULT 'KH',
    latitude      numeric(9,6) CHECK (latitude BETWEEN -90 AND 90),
    longitude     numeric(9,6) CHECK (longitude BETWEEN -180 AND 180),
    timezone      text NOT NULL DEFAULT 'Asia/Phnom_Penh',
    created_by    uuid NOT NULL REFERENCES app_user (id),
    opened_at     timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now(),
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    UNIQUE (school_id, name),
    UNIQUE (school_id, id)
);
CREATE UNIQUE INDEX branch_one_default ON branch (school_id) WHERE is_default;

CREATE TABLE reserved_slug (
    slug  text PRIMARY KEY
);

CREATE TABLE workspace (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   uuid NOT NULL,
    branch_id   uuid NOT NULL,
    slug        text NOT NULL
                CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$' AND slug !~ '--'),
    created_by  uuid NOT NULL REFERENCES app_user (id),
    created_at  timestamptz NOT NULL DEFAULT now(),
    retired_at  timestamptz,
    FOREIGN KEY (school_id, branch_id) REFERENCES branch (school_id, id)
);
CREATE UNIQUE INDEX workspace_slug_uq ON workspace (slug);
CREATE UNIQUE INDEX workspace_one_current ON workspace (branch_id) WHERE retired_at IS NULL;

CREATE FUNCTION resolve_workspace(p_slug text)
RETURNS TABLE (school_id uuid, branch_id uuid, canonical_slug text)
LANGUAGE sql STABLE AS $$
    SELECT w.school_id, w.branch_id, cur.slug
    FROM workspace w
    JOIN branch b      ON b.id = w.branch_id
    JOIN school s      ON s.id = w.school_id
    JOIN workspace cur ON cur.branch_id = w.branch_id AND cur.retired_at IS NULL
    WHERE w.slug = lower(btrim(p_slug))
      AND s.status = 'approved'
      AND b.status = 'active';
$$;
