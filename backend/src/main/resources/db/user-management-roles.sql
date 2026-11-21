BEGIN;

ALTER TABLE roles ADD CONSTRAINT roles_reserved_codes CHECK (
    (code <> 'SUPERADMIN' OR (tenant_id IS NULL AND scope = 'PLATFORM' AND is_system))
    AND (code <> 'TENANT_ADMIN' OR (tenant_id IS NOT NULL AND scope = 'TENANT' AND is_system))
);
CREATE UNIQUE INDEX roles_tenant_code_idx ON roles (tenant_id, code) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX roles_superadmin_idx ON roles (code) WHERE code = 'SUPERADMIN';

INSERT INTO roles (id, code, name, scope, is_system, created_at)
VALUES (gen_random_uuid(), 'SUPERADMIN', 'Superadmin', 'PLATFORM', true, now());

INSERT INTO permissions (id, code, module)
VALUES
    (gen_random_uuid(), 'users.create', 'users'),
    (gen_random_uuid(), 'users.read', 'users'),
    (gen_random_uuid(), 'users.role.assign', 'users'),
    (gen_random_uuid(), 'roles.create', 'roles'),
    (gen_random_uuid(), 'roles.permissions.update', 'roles'),
    (gen_random_uuid(), 'tenants.provision', 'tenants')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPERADMIN' AND p.code IN ('users.create', 'tenants.provision');

CREATE FUNCTION protect_superadmin_role() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'The SUPERADMIN role is protected' USING ERRCODE = 'check_violation';
END;
$$;

CREATE TRIGGER superadmin_role_delete
BEFORE DELETE ON roles
FOR EACH ROW WHEN (OLD.code = 'SUPERADMIN')
EXECUTE FUNCTION protect_superadmin_role();

CREATE TRIGGER superadmin_role_identity
BEFORE UPDATE OF id, code, tenant_id, scope, is_system ON roles
FOR EACH ROW WHEN (
    OLD.code = 'SUPERADMIN'
    AND ROW(OLD.id, OLD.code, OLD.tenant_id, OLD.scope, OLD.is_system)
        IS DISTINCT FROM ROW(NEW.id, NEW.code, NEW.tenant_id, NEW.scope, NEW.is_system)
)
EXECUTE FUNCTION protect_superadmin_role();

CREATE TRIGGER superadmin_role_truncate
BEFORE TRUNCATE ON roles
FOR EACH STATEMENT EXECUTE FUNCTION protect_superadmin_role();

COMMIT;
