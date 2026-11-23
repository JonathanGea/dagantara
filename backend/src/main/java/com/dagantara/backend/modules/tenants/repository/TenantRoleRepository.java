package com.dagantara.backend.modules.tenants.repository;

import com.dagantara.backend.modules.tenants.dto.TenantRoleResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRoleRepository {
    private static final String ROLE_SELECT = """
            SELECT r.id, r.tenant_id, r.code, r.name,
                   ARRAY(SELECT p.code FROM role_permissions rp
                         JOIN permissions p ON p.id = rp.permission_id
                         WHERE rp.role_id = r.id ORDER BY p.code) AS permissions
            FROM roles r WHERE r.tenant_id = :tenantId
            """;

    private final JdbcClient jdbc;

    public TenantRoleRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Page<TenantRoleResponse> list(UUID tenantId, Pageable page) {
        List<TenantRoleResponse> items = jdbc.sql(ROLE_SELECT + """
                ORDER BY r.code, r.id
                LIMIT :size OFFSET :offset
                """).param("tenantId", tenantId).param("size", page.getPageSize())
                .param("offset", page.getOffset()).query(TenantRoleRepository::map).list();
        long total = jdbc.sql("SELECT count(*) FROM roles WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId).query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public Optional<TenantRoleResponse> find(UUID tenantId, UUID id) {
        return jdbc.sql(ROLE_SELECT + "AND r.id = :id")
                .param("tenantId", tenantId).param("id", id).query(TenantRoleRepository::map).optional();
    }

    public UUID insert(UUID tenantId, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO roles (id, tenant_id, code, name) VALUES (:id, :tenantId, :code, :name)
                """).param("id", id).param("tenantId", tenantId).param("code", code).param("name", name).update();
        return id;
    }

    public void replacePermissions(UUID tenantId, UUID id, Set<String> codes) {
        jdbc.sql("""
                DELETE FROM role_permissions rp USING roles r
                WHERE rp.role_id = r.id AND r.tenant_id = :tenantId AND r.id = :id
                """).param("tenantId", tenantId).param("id", id).update();
        if (!codes.isEmpty()) {
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                    WHERE r.tenant_id = :tenantId AND r.id = :id AND p.code IN (:codes)
                    """).param("tenantId", tenantId).param("id", id).param("codes", codes).update();
        }
    }

    private static TenantRoleResponse map(ResultSet row, int index) throws SQLException {
        return new TenantRoleResponse(row.getObject("id", UUID.class), row.getObject("tenant_id", UUID.class),
                row.getString("code"), row.getString("name"),
                List.of((String[]) row.getArray("permissions").getArray()));
    }
}
