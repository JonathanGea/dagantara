BEGIN;

INSERT INTO permissions (id, code)
SELECT gen_random_uuid(), code
FROM (VALUES
    ('users.create'),
    ('users.read'),
    ('users.role.assign'),
    ('users.deactivate'),
    ('users.activate'),
    ('users.password.reset'),
    ('users.archive'),
    ('roles.create'),
    ('roles.permissions.update')
) AS tenant_permissions(code)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.tenant_id IS NOT NULL AND r.code = 'TENANT_ADMIN'
  AND p.code IN ('users.create', 'users.read', 'users.role.assign', 'users.deactivate',
                 'users.activate', 'users.password.reset', 'users.archive',
                 'roles.create', 'roles.permissions.update')
ON CONFLICT (role_id, permission_id) DO NOTHING;

ALTER TABLE roles ADD CONSTRAINT roles_tenant_id_key UNIQUE (tenant_id, id);

ALTER TABLE users ADD CONSTRAINT users_tenant_role_fk
    FOREIGN KEY (tenant_id, role_id) REFERENCES roles (tenant_id, id);

COMMIT;
