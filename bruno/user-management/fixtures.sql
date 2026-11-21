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
        'platform', 'Platform', 'PLATFORM', true, now());

INSERT INTO permissions (id, code, module)
VALUES ('00000000-0000-0000-0000-000000000201', 'users.create', 'users');

INSERT INTO role_permissions (role_id, permission_id)
VALUES ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000201');

WITH fixture_password AS MATERIALIZED (
    SELECT crypt(:'password', gen_salt('bf', 12)) AS hash
)
INSERT INTO users (id, tenant_id, role_id, username, email, full_name, password_hash,
                   account_type, status, created_at, updated_at)
SELECT id::uuid, '00000000-0000-0000-0000-000000000001', role_id::uuid,
       username, username || '@example.test', 'User Management Fixture', hash,
       'TENANT', 'ACTIVE', now(), now()
FROM fixture_password
CROSS JOIN (VALUES
    ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000101', 'creator'),
    ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000102', 'member')
) AS fixtures(id, role_id, username);
COMMIT;
