CREATE FUNCTION can_access(
    p_user         uuid,
    p_permission   text,
    p_school_id    uuid,
    p_branch_id    uuid DEFAULT NULL,
    p_target_user  uuid DEFAULT NULL,
    p_at           timestamptz DEFAULT now()
) RETURNS boolean
LANGUAGE sql STABLE AS $$
    WITH me AS (
        SELECT u.id FROM app_user u
        WHERE u.id = p_user AND u.is_active AND u.kind = 'person'
    ),
    target AS (
        SELECT s.status AS school_status, b.status AS branch_status
        FROM (SELECT 1) AS one
        LEFT JOIN school s ON s.id = p_school_id
        LEFT JOIN branch b ON b.id = p_branch_id AND b.school_id = p_school_id
    ),
    grants AS (
        SELECT rp.scope, NULL::uuid AS school_id, NULL::uuid AS branch_id, false AS is_support
        FROM role r
        JOIN role_permission rp ON rp.role_id = r.id AND rp.permission_code = p_permission
        WHERE r.kind = 'anyone'
           OR (r.kind = 'authenticated' AND EXISTS (SELECT 1 FROM me))
        UNION ALL
        SELECT rp.scope, ur.school_id, ur.branch_id, false
        FROM me
        JOIN user_role ur       ON ur.user_id = me.id
        JOIN role_permission rp ON rp.role_id = ur.role_id AND rp.permission_code = p_permission
        LEFT JOIN branch gb     ON gb.id = ur.branch_id
        WHERE ur.accepted_at IS NOT NULL
          AND tstzrange(ur.valid_from, ur.valid_to) @> p_at
          AND (ur.branch_id IS NULL OR gb.status = 'active')
        UNION ALL
        SELECT rp.scope, sa.school_id, NULL::uuid, true
        FROM me
        JOIN support_access sa  ON sa.user_id = me.id
        JOIN role r             ON r.kind = 'support'
        JOIN role_permission rp ON rp.role_id = r.id AND rp.permission_code = p_permission
        WHERE sa.ended_at IS NULL
          AND tstzrange(sa.started_at, sa.expires_at) @> p_at
          AND EXISTS (
              SELECT 1
              FROM user_role ur
              JOIN role_permission sp ON sp.role_id = ur.role_id
                                     AND sp.permission_code = 'support:start'
              WHERE ur.user_id = me.id
                AND ur.accepted_at IS NOT NULL
                AND tstzrange(ur.valid_from, ur.valid_to) @> p_at)
    )
    SELECT EXISTS (
        SELECT 1
        FROM grants g
        CROSS JOIN target t
        WHERE (p_school_id IS NULL OR t.school_status IS NOT NULL)
          AND (p_branch_id IS NULL OR t.branch_status IS NOT NULL)
          AND CASE g.scope
              WHEN 'platform' THEN true
              WHEN 'own' THEN p_target_user = p_user
              WHEN 'public' THEN
                  p_target_user IS NULL
                  AND t.school_status = 'approved'
                  AND (p_branch_id IS NULL OR t.branch_status = 'active')
              WHEN 'tenant' THEN
                  g.school_id = p_school_id
                  AND (t.school_status <> 'suspended' OR g.is_support)
                  AND (g.branch_id IS NULL
                       OR g.branch_id = p_branch_id
                       OR (p_branch_id IS NULL AND p_permission ~ ':(read|view)$'))
              ELSE false
          END
    );
$$;

CREATE FUNCTION can_grant_role(
    p_granter    uuid,
    p_role_id    uuid,
    p_school_id  uuid,
    p_branch_id  uuid,
    p_at         timestamptz DEFAULT now()
) RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT EXISTS (
        SELECT 1
        FROM user_role g
        JOIN app_user u        ON u.id = g.user_id AND u.is_active AND u.kind = 'person'
        JOIN role_grant_rule r ON r.granter_role_id = g.role_id
                              AND r.grantee_role_id = p_role_id
        LEFT JOIN school s     ON s.id = g.school_id
        LEFT JOIN branch gb    ON gb.id = g.branch_id
        WHERE g.user_id = p_granter
          AND g.accepted_at IS NOT NULL
          AND tstzrange(g.valid_from, g.valid_to) @> p_at
          AND (g.school_id IS NULL
               OR (g.school_id = p_school_id
                   AND s.status NOT IN ('suspended', 'rejected')
                   AND (g.branch_id IS NULL
                        OR (g.branch_id = p_branch_id AND gb.status = 'active'))))
    );
$$;

CREATE FUNCTION can_view_asset(
    p_user      uuid,
    p_asset_id  uuid,
    p_at        timestamptz DEFAULT now()
) RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT EXISTS (
        SELECT 1
        FROM media_asset a
        WHERE a.id = p_asset_id
          AND (
              (a.uploaded_by = p_user
               AND a.status NOT IN ('deleted', 'quarantined')
               AND CASE WHEN a.school_id IS NULL
                        THEN EXISTS (SELECT 1 FROM app_user u WHERE u.id = p_user AND u.is_active)
                        ELSE can_access(p_user, 'media:upload', a.school_id, a.branch_id, NULL, p_at)
                   END)
              OR (a.status = 'ready' AND (
                  (a.school_id IS NULL AND a.visibility = 'public')
                  OR (a.school_id IS NOT NULL AND can_access(p_user,
                         CASE a.visibility WHEN 'public'  THEN 'media.public:view'
                                           WHEN 'members' THEN 'media.members:view'
                                           ELSE 'media.staff:view' END,
                         a.school_id, a.branch_id, NULL, p_at))
                  OR (a.purpose IN ('school_logo', 'school_cover')
                      AND can_access(p_user, 'school:review', a.school_id, NULL, NULL, p_at))))
          )
    );
$$;

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
