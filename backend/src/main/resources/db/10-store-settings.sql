BEGIN;

INSERT INTO permissions (id, code)
VALUES (gen_random_uuid(), 'store_settings.manage')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.tenant_id IS NOT NULL AND r.code = 'TENANT_ADMIN'
  AND p.code = 'store_settings.manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;

CREATE TABLE store_settings (
    tenant_id uuid PRIMARY KEY REFERENCES tenants (id),
    display_name varchar(255),
    public_whatsapp varchar(16),
    public_email text
);

COMMIT;
