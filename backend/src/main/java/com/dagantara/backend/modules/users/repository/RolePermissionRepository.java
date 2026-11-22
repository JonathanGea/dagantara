package com.dagantara.backend.modules.users.repository;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RolePermissionRepository {
    private final JdbcClient jdbc;

    public RolePermissionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasPlatformPermission(UUID accountId, String permission) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM users u
                    JOIN roles r ON r.id = u.role_id
                    JOIN role_permissions rp ON rp.role_id = r.id
                    JOIN permissions p ON p.id = rp.permission_id
                    WHERE u.id = :accountId AND u.tenant_id IS NULL AND r.tenant_id IS NULL
                      AND p.code = :permission
                )
                """).param("accountId", accountId).param("permission", permission)
                .query(Boolean.class).single();
    }

    public UUID createTenantAdminRole(UUID tenantId) {
        UUID roleId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO roles (id, tenant_id, code, name)
                VALUES (:id, :tenantId, 'TENANT_ADMIN', 'Admin Tenant')
                """).param("id", roleId).param("tenantId", tenantId).update();
        return roleId;
    }
}
