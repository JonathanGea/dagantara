DO $$
DECLARE
    superadmin_role uuid;
    first_superadmin uuid;
    provisioned_tenant uuid;
    tenant_admin_role uuid;
    delegated_role uuid;
BEGIN
    IF (SELECT count(*) FROM users WHERE username = 'created-account') <> 1
       OR NOT EXISTS (
           SELECT 1 FROM users
           WHERE username = 'created-account'
             AND tenant_id = '00000000-0000-0000-0000-000000000001'
             AND role_id = '00000000-0000-0000-0000-000000000102'
             AND created_by = '00000000-0000-0000-0000-000000000301'
             AND account_type = 'TENANT' AND status = 'ACTIVE'
       ) THEN
        RAISE EXCEPTION 'UM-AT-01: incorrect persisted account or duplicate';
    END IF;

    IF EXISTS (
        SELECT 1 FROM users WHERE username IN ('denied-account', 'anonymous-account')
    ) THEN
        RAISE EXCEPTION 'UM-AT-02: a rejected account was persisted';
    END IF;

    IF EXISTS (
        SELECT 1 FROM users WHERE username IN (
            'cross-tenant-account', 'wrong-scope-account', 'wrong-platform-account'
        )
    ) OR NOT EXISTS (
        SELECT 1 FROM users WHERE username = 'member'
          AND role_id = '00000000-0000-0000-0000-000000000102'
    ) THEN
        RAISE EXCEPTION 'UM-AT-05: invalid role operation changed an account';
    END IF;

    SELECT id INTO STRICT delegated_role FROM roles WHERE code = 'AT03_DELEGATE'
        AND tenant_id = '00000000-0000-0000-0000-000000000001';
    IF (SELECT array_agg(p.code::text ORDER BY p.code)
        FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
        WHERE rp.role_id = delegated_role) IS DISTINCT FROM ARRAY['users.create', 'users.read']
       OR NOT EXISTS (SELECT 1 FROM users WHERE username = 'role-target' AND role_id = delegated_role)
       OR NOT EXISTS (
           SELECT 1 FROM users WHERE username = 'at03-created'
             AND created_by = '00000000-0000-0000-0000-000000000304'
       ) THEN
        RAISE EXCEPTION 'UM-AT-03: role grants or assignment were not persisted';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM users WHERE username = 'member-b'
          AND role_id = '00000000-0000-0000-0000-000000000104'
    ) THEN
        RAISE EXCEPTION 'UM-AT-04: role of another tenant user was changed';
    END IF;

    SELECT id INTO STRICT superadmin_role FROM roles
        WHERE code = 'SUPERADMIN' AND is_system AND scope = 'PLATFORM' AND tenant_id IS NULL;
    SELECT id INTO STRICT first_superadmin FROM users
        WHERE role_id = superadmin_role AND tenant_id IS NULL AND username = 'fixture-superadmin';
    IF (SELECT count(*) FROM users WHERE role_id = superadmin_role) <> 1 THEN
        RAISE EXCEPTION 'UM-AT-06: bootstrap must create exactly one initial superadmin';
    END IF;
    BEGIN
        UPDATE users SET created_by = NULL WHERE created_by = first_superadmin;
        DELETE FROM users WHERE role_id = superadmin_role;
        DELETE FROM role_permissions WHERE role_id = superadmin_role;
        DELETE FROM roles WHERE id = superadmin_role;
        RAISE EXCEPTION 'UM-AT-06: deletion of the unreferenced SUPERADMIN role was allowed';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
    BEGIN
        UPDATE roles SET code = 'RENAMED_SUPERADMIN' WHERE id = superadmin_role;
        RAISE EXCEPTION 'UM-AT-06: changing the protected role identity was allowed';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    SELECT id INTO STRICT provisioned_tenant FROM tenants
        WHERE code = 'at07-provisioned' AND status = 'ACTIVE';
    SELECT id INTO STRICT tenant_admin_role FROM roles
        WHERE code = 'TENANT_ADMIN' AND is_system AND scope = 'TENANT' AND tenant_id = provisioned_tenant;
    IF (SELECT array_agg(p.code::text ORDER BY p.code)
        FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
        WHERE rp.role_id = tenant_admin_role) IS DISTINCT FROM
            ARRAY['roles.create', 'roles.permissions.update', 'users.create', 'users.read', 'users.role.assign']
       OR NOT EXISTS (
           SELECT 1 FROM users WHERE username = 'initial-admin' AND tenant_id = provisioned_tenant
             AND role_id = tenant_admin_role AND created_by = first_superadmin
       )
       OR NOT EXISTS (SELECT 1 FROM users WHERE username = 'at07-created' AND tenant_id = provisioned_tenant)
       OR EXISTS (SELECT 1 FROM tenants WHERE code IN ('at07-rollback', 'at07-denied')) THEN
        RAISE EXCEPTION 'UM-AT-07: tenant provisioning state is incorrect';
    END IF;

    IF EXISTS (
        SELECT 1 FROM roles r JOIN role_permissions rp ON rp.role_id = r.id
        WHERE r.code = 'AT08_LIMITED'
    ) OR EXISTS (SELECT 1 FROM users WHERE username = 'at08-denied') THEN
        RAISE EXCEPTION 'UM-AT-08: denied delegation changed persisted state';
    END IF;

    BEGIN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT role_id, permission_id FROM role_permissions;
        RAISE EXCEPTION 'UM-BR-06: duplicate role permission was allowed';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;

    BEGIN
        UPDATE users
        SET role_id = '00000000-0000-0000-0000-000000000999'
        WHERE username = 'created-account';
        RAISE EXCEPTION 'UM-BR-02: role_id did not require an existing role';
    EXCEPTION WHEN foreign_key_violation THEN
        NULL;
    END;

    BEGIN
        UPDATE users SET role_id = NULL WHERE username = 'created-account';
        RAISE EXCEPTION 'UM-BR-02: role_id allowed NULL';
    EXCEPTION WHEN not_null_violation THEN
        NULL;
    END;
END;
$$;
