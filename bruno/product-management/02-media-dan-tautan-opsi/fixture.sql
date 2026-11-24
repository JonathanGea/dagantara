-- Run once on the disposable Product 02 database after migrations 01 through 07.
\getenv fixture_password PRODUCT02_FIXTURE_PASSWORD
\getenv fixture_host PRODUCT02_FIXTURE_HOST

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMP TABLE fixture_accounts ON COMMIT DROP AS
SELECT label, '/pm02-' || label AS shop_path, gen_random_uuid() AS tenant_id,
       gen_random_uuid() AS owner_id, gen_random_uuid() AS manager_role
FROM (VALUES ('a'), ('b')) AS tenants(label);

INSERT INTO tenants (id, name, owner_user_id)
SELECT tenant_id, 'Product 02 fixture ' || label, owner_id FROM fixture_accounts;

INSERT INTO tenant_domains (hostname, shop_path, tenant_id)
SELECT :'fixture_host', shop_path, tenant_id FROM fixture_accounts;

INSERT INTO roles (id, tenant_id, code, name)
SELECT manager_role, tenant_id, 'TENANT_ADMIN', 'Tenant admin' FROM fixture_accounts
UNION ALL
SELECT gen_random_uuid(), tenant_id, 'CATALOG_READER', 'Catalog reader' FROM fixture_accounts WHERE label = 'a';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM fixture_accounts f
JOIN roles r ON r.tenant_id = f.tenant_id
CROSS JOIN permissions p
WHERE p.code = 'catalog.read' OR (p.code = 'catalog.manage' AND r.code = 'TENANT_ADMIN');

INSERT INTO users (id, tenant_id, role_id, username, password_hash)
SELECT owner_id, tenant_id, manager_role, 'pm02-manager', crypt(:'fixture_password', gen_salt('bf', 10))
FROM fixture_accounts
UNION ALL
SELECT gen_random_uuid(), f.tenant_id, r.id, 'pm02-reader', crypt(:'fixture_password', gen_salt('bf', 10))
FROM fixture_accounts f JOIN roles r ON r.tenant_id = f.tenant_id AND r.code = 'CATALOG_READER';

COMMIT;
