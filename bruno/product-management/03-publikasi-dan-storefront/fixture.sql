-- Apply after migrations 01–10 on an isolated database. The platform host is localhost.
\getenv fixture_password PRODUCT03_FIXTURE_PASSWORD

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMP TABLE fixture_accounts ON COMMIT DROP AS
SELECT hostname, shop_path, gen_random_uuid() AS tenant_id,
       gen_random_uuid() AS owner_id, gen_random_uuid() AS manager_role
FROM (VALUES ('localhost', '/pm03'), ('127.0.0.1', '')) AS addresses(hostname, shop_path);

INSERT INTO tenants (id, name, owner_user_id)
SELECT tenant_id, 'Product 03 ' || hostname, owner_id FROM fixture_accounts;

INSERT INTO tenant_domains (hostname, shop_path, tenant_id)
SELECT hostname, shop_path, tenant_id FROM fixture_accounts;

INSERT INTO roles (id, tenant_id, code, name)
SELECT manager_role, tenant_id, 'TENANT_ADMIN', 'Tenant admin' FROM fixture_accounts
UNION ALL
SELECT gen_random_uuid(), tenant_id, 'CATALOG_READER', 'Catalog reader' FROM fixture_accounts WHERE shop_path <> '';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM fixture_accounts f
JOIN roles r ON r.tenant_id = f.tenant_id
CROSS JOIN permissions p
WHERE p.code = 'catalog.read'
   OR (p.code IN ('catalog.manage', 'inventory.read', 'inventory.receive', 'inventory.correct')
       AND r.code = 'TENANT_ADMIN');

INSERT INTO users (id, tenant_id, role_id, username, password_hash)
SELECT owner_id, tenant_id, manager_role, 'pm03-manager', crypt(:'fixture_password', gen_salt('bf', 10))
FROM fixture_accounts
UNION ALL
SELECT gen_random_uuid(), f.tenant_id, r.id, 'pm03-reader', crypt(:'fixture_password', gen_salt('bf', 10))
FROM fixture_accounts f JOIN roles r ON r.tenant_id = f.tenant_id AND r.code = 'CATALOG_READER';

COMMIT;
