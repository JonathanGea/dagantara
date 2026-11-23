BEGIN;

ALTER TABLE users ADD COLUMN status varchar(16) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT users_lifecycle_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    ADD CONSTRAINT users_initial_admin_is_active CHECK (NOT is_initial_platform_admin OR status = 'ACTIVE');

WITH platform_permissions(code) AS (
    VALUES ('users.create'), ('users.read'), ('users.role.assign'), ('users.password.reset'),
           ('users.deactivate'), ('users.activate'), ('users.archive'), ('roles.create'),
           ('roles.permissions.update'), ('tenants.admins.password.reset')
), inserted_permissions AS (
    INSERT INTO permissions (id, code)
    SELECT gen_random_uuid(), code FROM platform_permissions
    RETURNING id
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN inserted_permissions p
WHERE r.tenant_id IS NULL AND r.code = 'SUPERADMIN';

COMMIT;
