-- Run on the isolated test database after migrations 01, 02, and 03.
BEGIN;

INSERT INTO tenants (id, name, owner_user_id)
VALUES ('03000000-0000-4000-8000-000000000001', 'UM-AT-05 scope fixture',
        '03000000-0000-4000-8000-000000000002')
ON CONFLICT (id) DO NOTHING;

INSERT INTO roles (id, tenant_id, code, name)
VALUES ('03000000-0000-4000-8000-000000000003', '03000000-0000-4000-8000-000000000001',
        'TENANT_ADMIN', 'Admin Tenant')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, tenant_id, role_id, username, password_hash)
VALUES ('03000000-0000-4000-8000-000000000002', '03000000-0000-4000-8000-000000000001',
        '03000000-0000-4000-8000-000000000003', 'um03-tenant-scope-fixture', 'unusable-fixture-hash')
ON CONFLICT (id) DO NOTHING;

COMMIT;
