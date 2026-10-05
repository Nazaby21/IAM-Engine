INSERT INTO permission (code, description, is_sensitive) VALUES
    ('school:review',          'Approve, reject or request changes to a school',   false),
    ('school:suspend',         'Suspend or reinstate a school',                    false),
    ('school.directory:read',  'List all schools and their status',                false),
    ('workspace:manage',       'Retire any workspace name',                        false),
    ('user:suspend',           'Deactivate an account platform-wide',              false),
    ('support:start',          'Open a read-only support session on a school',     true),
    ('media:moderate',         'Quarantine any file',                              false),
    ('platform.audit:read',    'Read the platform audit trail',                    true),
    ('school.profile:read',    'See the school profile',                           false),
    ('school.profile:edit',    'Edit the school profile, logo and cover',          false),
    ('school:submit',          'Submit the school for review',                     false),
    ('branch:read',            'See a branch',                                     false),
    ('branch:create',          'Add branches',                                     false),
    ('branch:open',            'Open or close branches, choose the default',       false),
    ('branch:edit',            'Edit branch details and photos',                   false),
    ('workspace:edit',         'Create or retire workspace names',                 false),
    ('member:read',            'See members and their roles',                      true),
    ('media:upload',           'Upload images, video and audio',                   false),
    ('media:manage',           'Change visibility of or delete anyone''s files',   false),
    ('media.public:view',      'See public files',                                 false),
    ('media.members:view',     'See members-only files',                           false),
    ('media.staff:view',       'See staff-only files',                             false),
    ('audit_log:read',         'Read the school''s audit trail',                   true);

INSERT INTO role (code, name, kind, allows_branch, pre_approval) VALUES
    ('visitor',        'Anyone, signed in or not',      'anyone',        false, false),
    ('member',         'Any signed-in person',          'authenticated', false, false),
    ('super_admin',    'Super admin',                   'platform',      false, false),
    ('support_viewer', 'Support session (read-only)',   'support',       false, false),
    ('school_owner',   'School owner',                  'school',        false, true),
    ('school_admin',   'School admin',                  'school',        true,  true),
    ('instructor',     'Instructor',                    'school',        true,  false),
    ('student',        'Student',                       'school',        true,  false);

INSERT INTO role_permission (role_id, permission_code, scope)
SELECT r.id, p.code, v.scope::access_scope
FROM (VALUES
    ('visitor',        'public',   ARRAY['school.profile:read', 'branch:read', 'media.public:view']),
    ('member',         'own',      ARRAY['member:read']),
    ('super_admin',    'platform', ARRAY['school:review', 'school:suspend', 'school.directory:read',
                                         'workspace:manage', 'user:suspend', 'support:start',
                                         'media:moderate', 'platform.audit:read']),
    ('support_viewer', 'tenant',   ARRAY['school.profile:read', 'branch:read', 'member:read',
                                         'media.public:view', 'media.members:view', 'media.staff:view',
                                         'audit_log:read']),
    ('school_owner',   'tenant',   ARRAY['school.profile:read', 'school.profile:edit', 'school:submit',
                                         'branch:read', 'branch:create', 'branch:open', 'branch:edit',
                                         'workspace:edit', 'member:read', 'media:upload', 'media:manage',
                                         'media.public:view', 'media.members:view', 'media.staff:view',
                                         'audit_log:read']),
    ('school_admin',   'tenant',   ARRAY['school.profile:read', 'school.profile:edit', 'school:submit',
                                         'branch:read', 'branch:create', 'branch:open', 'branch:edit',
                                         'workspace:edit', 'member:read', 'media:upload', 'media:manage',
                                         'media.public:view', 'media.members:view', 'media.staff:view',
                                         'audit_log:read']),
    ('instructor',     'tenant',   ARRAY['school.profile:read', 'branch:read', 'member:read', 'media:upload',
                                         'media.public:view', 'media.members:view', 'media.staff:view']),
    ('student',        'tenant',   ARRAY['school.profile:read', 'branch:read',
                                         'media.public:view', 'media.members:view'])
) AS v (role_code, scope, perms)
JOIN role r ON r.code = v.role_code
CROSS JOIN LATERAL unnest(v.perms) AS p (code);

INSERT INTO role_grant_rule (granter_role_id, grantee_role_id)
SELECT g.id, e.id
FROM (VALUES
    ('super_admin',  'super_admin'),
    ('super_admin',  'school_owner'),
    ('school_owner', 'school_owner'),
    ('school_owner', 'school_admin'),
    ('school_owner', 'instructor'),
    ('school_owner', 'student'),
    ('school_admin', 'instructor'),
    ('school_admin', 'student')
) AS v (granter, grantee)
JOIN role g ON g.code = v.granter
JOIN role e ON e.code = v.grantee;

INSERT INTO reserved_slug (slug)
SELECT unnest(ARRAY[
    'www', 'api', 'app', 'apps', 'admin', 'administrator', 'root', 'system', 'internal',
    'auth', 'login', 'logout', 'signin', 'signup', 'register', 'account', 'accounts', 'sso', 'oauth',
    'billing', 'pay', 'payment', 'payments', 'checkout',
    'help', 'support', 'status', 'docs', 'blog', 'news', 'about', 'contact', 'legal', 'terms', 'privacy',
    'security', 'abuse', 'trust',
    'mail', 'email', 'smtp', 'imap', 'pop', 'ftp', 'autodiscover', 'autoconfig',
    'cdn', 'static', 'assets', 'media', 'files', 'upload', 'uploads', 'img', 'images', 'video', 'videos',
    'dev', 'staging', 'stage', 'test', 'testing', 'beta', 'demo', 'sandbox', 'preview',
    'dashboard', 'console', 'portal', 'workspace', 'workspaces', 'school', 'schools',
    'krobkrong', 'official', 'ns1', 'ns2', 'ns3']);
