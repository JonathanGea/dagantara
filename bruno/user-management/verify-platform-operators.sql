-- Run after the platform Bruno scenarios, on the same isolated PostgreSQL database.
DO $$
BEGIN
    IF EXISTS (SELECT FROM users WHERE username LIKE 'um03-denied-%')
       OR EXISTS (SELECT FROM roles WHERE code LIKE 'um03-denied-%') THEN
        RAISE EXCEPTION 'A rejected platform write was persisted (UM-AT-02/03/05/08/12)';
    END IF;

    IF NOT EXISTS (
        SELECT FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.username LIKE 'um03-new-operator-%' AND u.tenant_id IS NULL AND r.tenant_id IS NULL
          AND u.status = 'ACTIVE' AND NOT u.password_change_required AND u.session_version = 1
          AND u.password_hash ~ '^\$2[aby]\$12\$[./A-Za-z0-9]{53}$'
    ) THEN
        RAISE EXCEPTION 'No active operator with a scoped role and a replaced credential (UM-AT-01)';
    END IF;

    IF NOT EXISTS (
        SELECT FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.id = '03000000-0000-4000-8000-000000000002'
          AND u.role_id = '03000000-0000-4000-8000-000000000003'
          AND u.tenant_id = '03000000-0000-4000-8000-000000000001' AND r.tenant_id = u.tenant_id
          AND u.status = 'ACTIVE' AND u.session_version = 0 AND u.password_hash = 'unusable-fixture-hash'
    ) THEN
        RAISE EXCEPTION 'The cross-scope tenant fixture is missing or changed (UM-AT-05)';
    END IF;

    IF (SELECT count(*) FROM users WHERE is_initial_platform_admin) <> 1
       OR NOT EXISTS (
           SELECT FROM users u JOIN roles r ON r.id = u.role_id
           WHERE u.is_initial_platform_admin AND u.tenant_id IS NULL AND r.tenant_id IS NULL
             AND r.code = 'SUPERADMIN' AND u.status = 'ACTIVE' AND u.session_version = 0
       ) THEN
        RAISE EXCEPTION 'The initial platform administrator is missing or changed (UM-AT-06/12)';
    END IF;

    IF NOT EXISTS (
        SELECT FROM tenants t JOIN users u ON u.id = t.owner_user_id
        JOIN tenant_domains d ON d.tenant_id = t.id
        WHERE t.name LIKE 'um03-provision-only-%' AND d.shop_path = '/' || t.name AND u.username = t.name
          AND u.tenant_id = t.id AND u.status = 'ACTIVE'
    ) THEN
        RAISE EXCEPTION 'The delegated provisioning operation did not persist (UM-AT-08)';
    END IF;

    IF NOT EXISTS (
        SELECT FROM roles r JOIN role_permissions rp ON rp.role_id = r.id
        JOIN permissions p ON p.id = rp.permission_id
        WHERE r.code LIKE 'um03-help-center-only-%' AND r.tenant_id IS NULL
        GROUP BY r.id HAVING array_agg(p.code::text) = ARRAY['tenants.admins.password.reset']
    ) OR NOT EXISTS (
        SELECT FROM roles r JOIN role_permissions rp ON rp.role_id = r.id
        JOIN permissions p ON p.id = rp.permission_id
        WHERE r.code LIKE 'um03-provisioning-only-%' AND r.tenant_id IS NULL
        GROUP BY r.id HAVING array_agg(p.code::text) = ARRAY['tenants.provision']
    ) THEN
        RAISE EXCEPTION 'Provisioning and Help Center permissions were not stored separately (UM-AT-08)';
    END IF;

    IF NOT EXISTS (
        SELECT FROM users u JOIN roles r ON r.id = u.role_id
        WHERE u.username LIKE 'um03-lifecycle-target-%' AND r.code LIKE 'um03-lifecycle-reader-%'
          AND u.tenant_id IS NULL AND r.tenant_id IS NULL AND u.status = 'ARCHIVED'
          AND u.session_version = 6 AND NOT u.password_change_required
    ) THEN
        RAISE EXCEPTION 'Archive did not retain account identity, role, and revoked session (UM-AT-11)';
    END IF;
END;
$$;
