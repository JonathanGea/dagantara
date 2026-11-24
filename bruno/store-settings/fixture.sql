-- Apply after migrations 01–09 and before migration 10 on an isolated database.
-- APP_PLATFORM_HOST=localhost. A uses 127.0.0.1 and B uses localhost/ss01-b.
\getenv fixture_password SS01_FIXTURE_PASSWORD

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMP TABLE fixture_accounts ON COMMIT DROP AS
SELECT hostname, shop_path, store_name, gen_random_uuid() AS tenant_id,
       gen_random_uuid() AS owner_id, gen_random_uuid() AS admin_role
FROM (VALUES ('127.0.0.1', '', 'Kopi Pagi Nusantara'),
             ('localhost', '/ss01-b', 'Teh Sore Nusantara')) AS stores(hostname, shop_path, store_name);

INSERT INTO tenants (id, name, owner_user_id)
SELECT tenant_id, store_name, owner_id FROM fixture_accounts;

INSERT INTO tenant_domains (hostname, shop_path, tenant_id)
SELECT hostname, shop_path, tenant_id FROM fixture_accounts;

INSERT INTO roles (id, tenant_id, code, name)
SELECT admin_role, tenant_id, 'TENANT_ADMIN', 'Admin Tenant' FROM fixture_accounts
UNION ALL
SELECT gen_random_uuid(), tenant_id, 'SS01_LIMITED', 'Pengelola role' FROM fixture_accounts WHERE shop_path = '';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM fixture_accounts f
JOIN roles r ON r.tenant_id = f.tenant_id
CROSS JOIN permissions p
WHERE p.code = 'roles.permissions.update'
   OR (r.code = 'TENANT_ADMIN' AND p.code IN ('catalog.read', 'catalog.manage', 'roles.create'));

INSERT INTO users (id, tenant_id, role_id, username, password_hash, phone_number)
SELECT owner_id, tenant_id, admin_role, 'ss01-owner', crypt(:'fixture_password', gen_salt('bf', 10)), '+6281100000000'
FROM fixture_accounts
UNION ALL
SELECT gen_random_uuid(), f.tenant_id, r.id, 'ss01-limited', crypt(:'fixture_password', gen_salt('bf', 10)), NULL
FROM fixture_accounts f JOIN roles r ON r.tenant_id = f.tenant_id AND r.code = 'SS01_LIMITED'
UNION ALL
SELECT gen_random_uuid(), NULL, id, 'ss01-platform', crypt(:'fixture_password', gen_salt('bf', 10)), NULL
FROM roles WHERE tenant_id IS NULL AND code = 'SUPERADMIN';

COMMIT;
