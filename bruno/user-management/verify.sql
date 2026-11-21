DO $$
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
    ) THEN
        RAISE EXCEPTION 'UM-AT-05: a rejected account was persisted';
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
