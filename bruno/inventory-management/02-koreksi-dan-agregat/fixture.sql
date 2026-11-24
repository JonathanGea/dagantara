-- Apply migrations 01–09 to a disposable database before loading this fixture.
-- Export INVENTORY02_FIXTURE_PASSWORD and a fresh INVENTORY02_FIXTURE_PREFIX (e.g. im02-01).
-- Run psql -X -v ON_ERROR_STOP=1 -f fixture.sql, then run this Bruno folder with the same variables.
\getenv fixture_password INVENTORY02_FIXTURE_PASSWORD
\getenv fixture_prefix INVENTORY02_FIXTURE_PREFIX

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMP TABLE fixture_tenants ON COMMIT DROP AS
SELECT label, '/' || :'fixture_prefix' || '-' || label AS shop_path, gen_random_uuid() AS tenant_id,
       gen_random_uuid() AS owner_id, gen_random_uuid() AS owner_role
FROM (VALUES ('a'), ('b')) AS tenants(label);

INSERT INTO tenants (id, name, owner_user_id)
SELECT tenant_id, 'Inventory 02 fixture ' || label, owner_id FROM fixture_tenants;

INSERT INTO tenant_domains (hostname, shop_path, tenant_id)
SELECT 'localhost', shop_path, tenant_id FROM fixture_tenants;

INSERT INTO roles (id, tenant_id, code, name)
SELECT owner_role, tenant_id, 'TENANT_ADMIN', 'Tenant Admin' FROM fixture_tenants
UNION ALL
SELECT gen_random_uuid(), tenant_id, 'INVENTORY_READER', 'Inventory Reader'
FROM fixture_tenants WHERE label = 'a';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM fixture_tenants f JOIN roles r ON r.tenant_id = f.tenant_id
CROSS JOIN permissions p
WHERE p.code = 'inventory.read'
   OR (r.code = 'TENANT_ADMIN' AND p.code IN ('inventory.receive', 'inventory.correct', 'catalog.read',
                                                           'catalog.manage'));

WITH credentials AS MATERIALIZED (
    SELECT crypt(:'fixture_password', gen_salt('bf', 10)) AS hash
)
INSERT INTO users (id, tenant_id, role_id, username, password_hash)
SELECT owner_id, tenant_id, owner_role, 'im02-owner', hash FROM fixture_tenants CROSS JOIN credentials
UNION ALL
SELECT gen_random_uuid(), f.tenant_id, r.id, 'im02-reader', hash
FROM fixture_tenants f JOIN roles r ON r.tenant_id = f.tenant_id AND r.code = 'INVENTORY_READER'
CROSS JOIN credentials;

-- A projected balance below the original quantity exercises the nonnegative guard.
-- Seed this state directly since outgoing stock movements are outside this release.
CREATE TEMP TABLE fixture_stock ON COMMIT DROP AS
SELECT tenant_id, owner_id, gen_random_uuid() AS category_id, gen_random_uuid() AS product_id,
       gen_random_uuid() AS variant_id, gen_random_uuid() AS warehouse_id, gen_random_uuid() AS receipt_id
FROM fixture_tenants WHERE label = 'a';

INSERT INTO categories (id, tenant_id, name)
SELECT category_id, tenant_id, 'Kaos' FROM fixture_stock;

INSERT INTO products (id, tenant_id, name, primary_category_id)
SELECT product_id, tenant_id, 'Kaos tanpa opsi', category_id FROM fixture_stock;

INSERT INTO product_categories (tenant_id, product_id, category_id)
SELECT tenant_id, product_id, category_id FROM fixture_stock;

INSERT INTO product_variants (id, tenant_id, product_id, sku, sku_mode, combination_key, first_stock_recorded_at)
SELECT variant_id, tenant_id, product_id, 'DEFAULT', 'MANUAL', ARRAY[]::uuid[], CURRENT_TIMESTAMP FROM fixture_stock;

INSERT INTO inventory_warehouses (id, tenant_id)
SELECT warehouse_id, tenant_id FROM fixture_stock;

INSERT INTO inventory_receipts (id, tenant_id, warehouse_id, received_by)
SELECT receipt_id, tenant_id, warehouse_id, owner_id FROM fixture_stock;

INSERT INTO inventory_receipt_lines (id, tenant_id, receipt_id, variant_id, sku, quantity)
SELECT gen_random_uuid(), tenant_id, receipt_id, variant_id, 'DEFAULT', 8 FROM fixture_stock;

INSERT INTO inventory_stock_balances (tenant_id, warehouse_id, variant_id, quantity)
SELECT tenant_id, warehouse_id, variant_id, 0 FROM fixture_stock;

COMMIT;
