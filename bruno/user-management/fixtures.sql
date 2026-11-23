\getenv password UM_TEST_PASSWORD
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO tenants (id, code, name, status, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'tenant-a', 'Fixture A', 'ACTIVE', now(), now()),
    ('00000000-0000-0000-0000-000000000002', 'tenant-b', 'Fixture B', 'ACTIVE', now(), now());

INSERT INTO roles (id, tenant_id, code, name, scope, is_system, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001',
        'creator', 'Creator', 'TENANT', false, now()),
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000001',
        'member', 'Member', 'TENANT', false, now()),
    ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000001',
        'wrong-scope', 'Wrong scope for tenant account', 'PLATFORM', false, now()),
    ('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000002',
        'member', 'Member', 'TENANT', false, now()),
    ('00000000-0000-0000-0000-000000000105', NULL,
        'platform', 'Platform', 'PLATFORM', true, now()),
    ('00000000-0000-0000-0000-000000000106', '00000000-0000-0000-0000-000000000001',
        'manager', 'Manager', 'TENANT', false, now()),
    ('00000000-0000-0000-0000-000000000107', '00000000-0000-0000-0000-000000000001',
        'limited', 'Limited Manager', 'TENANT', false, now());

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE (r.code = 'creator' AND p.code = 'users.create')
   OR (r.code = 'manager' AND p.code <> 'tenants.provision')
   OR (r.code = 'limited' AND p.code IN (
       'users.create', 'roles.create', 'roles.permissions.update', 'users.role.assign'
   ));

WITH fixture_password AS MATERIALIZED (
    SELECT crypt(:'password', gen_salt('bf', 12)) AS hash
)
INSERT INTO users (id, tenant_id, role_id, username, email, full_name, password_hash,
                   account_type, status, created_at, updated_at)
SELECT id::uuid, tenant_id::uuid, role_id::uuid,
       username, username || '@example.test', 'User Management Fixture', hash,
       'TENANT', 'ACTIVE', now(), now()
FROM fixture_password
CROSS JOIN (VALUES
    ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000101', 'creator'),
    ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000102', 'member'),
    ('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000106', 'manager'),
    ('00000000-0000-0000-0000-000000000304', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000102', 'role-target'),
    ('00000000-0000-0000-0000-000000000305', '00000000-0000-0000-0000-000000000002',
     '00000000-0000-0000-0000-000000000104', 'member-b'),
    ('00000000-0000-0000-0000-000000000306', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000107', 'limited')
) AS fixtures(id, tenant_id, role_id, username);
COMMIT;

-- Exercise first-account setup and its retry using temporary fixture credentials.
SELECT password_hash AS fixture_hash FROM users WHERE username = 'creator'
\gset
\setenv DAGANTARA_SUPERADMIN_USERNAME fixture-superadmin
\setenv DAGANTARA_SUPERADMIN_EMAIL fixture-superadmin@example.test
\setenv DAGANTARA_SUPERADMIN_FULL_NAME 'Fixture Superadmin'
\setenv DAGANTARA_SUPERADMIN_PASSWORD_HASH :fixture_hash
\ir ../../backend/src/main/resources/db/bootstrap-superadmin.sql
\ir ../../backend/src/main/resources/db/bootstrap-superadmin.sql
