BEGIN;

ALTER TABLE tenants
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT tenants_lifecycle_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

CREATE INDEX tenant_domains_tenant_address_idx ON tenant_domains (tenant_id, hostname, shop_path);

WITH inserted_permissions AS (
    INSERT INTO permissions (id, code)
    VALUES (gen_random_uuid(), 'tenants.read'),
           (gen_random_uuid(), 'tenants.deactivate'),
           (gen_random_uuid(), 'tenants.activate')
    RETURNING id
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN inserted_permissions p
WHERE r.tenant_id IS NULL AND r.code = 'SUPERADMIN';

COMMIT;
