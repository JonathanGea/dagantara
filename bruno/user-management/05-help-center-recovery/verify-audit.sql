-- Run after the Bruno scenario on the same isolated PostgreSQL database.
DO $$
BEGIN
    IF (SELECT array_agg(column_name::text ORDER BY ordinal_position)
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'tenant_admin_recovery_audit')
        IS DISTINCT FROM ARRAY['id', 'operator_id', 'tenant_id', 'target_user_id', 'verification_method', 'reset_at']
    THEN
        RAISE EXCEPTION 'Unexpected recovery audit columns (UM-AT-10)';
    END IF;

    IF NOT EXISTS (SELECT FROM tenants WHERE name LIKE 'um03-um05-shop-%') OR EXISTS (
        SELECT FROM tenants t
        WHERE t.name LIKE 'um03-um05-shop-%' AND (
            (SELECT count(*) FROM tenant_admin_recovery_audit a WHERE a.tenant_id = t.id) <> 2
            OR NOT EXISTS (
                SELECT FROM tenant_admin_recovery_audit a
                WHERE a.tenant_id = t.id AND a.target_user_id = t.owner_user_id
                  AND a.verification_method = 'MATCHED_INCOMING_NUMBER'
            )
            OR NOT EXISTS (
                SELECT FROM tenant_admin_recovery_audit a
                JOIN users target ON target.tenant_id = a.tenant_id AND target.id = a.target_user_id
                WHERE a.tenant_id = t.id AND target.id <> t.owner_user_id
                  AND target.username LIKE 'um03-um05-admin-%'
                  AND a.verification_method = 'CALLBACK_TO_REGISTERED_NUMBER'
            )
        )
    ) THEN
        RAISE EXCEPTION 'Expected exactly one owner reset and one additional admin reset per fixture (UM-AT-10)';
    END IF;

    IF EXISTS (
        SELECT FROM tenant_admin_recovery_audit a
        JOIN tenants t ON t.id = a.tenant_id
        JOIN users target ON target.tenant_id = a.tenant_id AND target.id = a.target_user_id
        JOIN roles target_role ON target_role.id = target.role_id
        JOIN users operator ON operator.id = a.operator_id
        JOIN roles operator_role ON operator_role.id = operator.role_id
        WHERE t.name NOT LIKE 'um03-um05-shop-%'
           OR target_role.code <> 'TENANT_ADMIN'
           OR target.status <> 'ACTIVE' OR target.password_change_required OR target.session_version <> 3
           OR target.password_hash !~ '^\$2[aby]\$12\$[./A-Za-z0-9]{53}$'
           OR operator.tenant_id IS NOT NULL OR operator_role.tenant_id IS NOT NULL
           OR operator.username <> replace(t.name, 'um05-shop-', 'um05-help-')
           OR (SELECT array_agg(p.code::text) FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
               WHERE rp.role_id = operator_role.id) IS DISTINCT FROM ARRAY['tenants.admins.password.reset']
           OR NOT isfinite(a.reset_at) OR a.reset_at > statement_timestamp()
    ) THEN
        RAISE EXCEPTION 'Recovery audit actor, target, timestamp, or resulting credential state is invalid (UM-AT-10)';
    END IF;
END;
$$;
