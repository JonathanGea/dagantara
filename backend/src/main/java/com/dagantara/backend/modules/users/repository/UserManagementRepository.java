package com.dagantara.backend.modules.users.repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserManagementRepository {
    private final JdbcClient jdbc;

    UserManagementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void lockActorTenant(UUID actorId) {
        jdbc.sql("""
                SELECT t.id FROM tenants t JOIN users u ON u.tenant_id = t.id
                WHERE u.id = :actorId FOR UPDATE OF t
                """).param("actorId", actorId).query(UUID.class).optional();
    }

    public Set<String> effectivePermissions(UUID actorId) {
        return new HashSet<>(jdbc.sql("""
                SELECT p.code FROM users u
                JOIN roles r ON r.id = u.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE u.id = :actorId AND u.status = 'ACTIVE'
                  AND r.tenant_id IS NOT DISTINCT FROM u.tenant_id AND r.scope = u.account_type
                """).param("actorId", actorId).query(String.class).list());
    }

    public Optional<Role> findRole(UUID roleId, UUID tenantId, String scope) {
        return jdbc.sql("""
                SELECT id, code, name, is_system FROM roles
                WHERE id = :roleId AND tenant_id IS NOT DISTINCT FROM CAST(:tenantId AS uuid)
                  AND scope = :scope
                """).param("roleId", roleId).param("tenantId", tenantId).param("scope", scope)
                .query((rs, row) -> new Role(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getBoolean("is_system"))).optional();
    }

    public Set<String> rolePermissions(UUID roleId) {
        return new HashSet<>(jdbc.sql("""
                SELECT p.code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = :roleId
                """).param("roleId", roleId).query(String.class).list());
    }

    public Role createTenantRole(UUID tenantId, String code, String name, boolean system) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO roles (id, tenant_id, code, name, scope, is_system, created_at)
                VALUES (:id, :tenantId, :code, :name, 'TENANT', :system, now())
                """).param("id", id).param("tenantId", tenantId).param("code", code)
                .param("name", name).param("system", system).update();
        return new Role(id, code, name, system);
    }

    public void replaceRolePermissions(UUID roleId, Set<String> codes) {
        jdbc.sql("DELETE FROM role_permissions WHERE role_id = :roleId").param("roleId", roleId).update();
        if (!codes.isEmpty()) {
            int inserted = jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT :roleId, id FROM permissions WHERE code IN (:codes)
                    """).param("roleId", roleId).param("codes", codes).update();
            if (inserted != codes.size()) {
                throw new IllegalStateException("The User Management permission catalog is incomplete");
            }
        }
    }

    public UUID createTenant(String code, String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO tenants (id, code, name, status, created_at, updated_at)
                VALUES (:id, :code, :name, 'ACTIVE', now(), now())
                """).param("id", id).param("code", code).param("name", name).update();
        return id;
    }

    public record Role(UUID id, String code, String name, boolean system) {
    }
}
