package com.dagantara.backend.modules.tenants.repository;

import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.Status;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
public class TenantAccountRepository {
    private static final String ACCOUNT_SELECT = """
            SELECT u.id, u.tenant_id, u.username, u.role_id, r.code AS role_code, u.status,
                   u.id = t.owner_user_id AS owner
            FROM users u
            JOIN roles r ON r.tenant_id = u.tenant_id AND r.id = u.role_id
            JOIN tenants t ON t.id = u.tenant_id
            WHERE u.tenant_id = :tenantId
            """;

    private final JdbcClient jdbc;

    public TenantAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void lockManagement(UUID tenantId) {
        jdbc.sql("SELECT id FROM tenants WHERE id = :tenantId FOR NO KEY UPDATE")
                .param("tenantId", tenantId).query(UUID.class).optional();
    }

    public Optional<Manager> findManager(AccountSession session) {
        return jdbc.sql("""
                SELECT u.id = t.owner_user_id AS owner,
                       ARRAY(SELECT p.code FROM role_permissions rp
                             JOIN permissions p ON p.id = rp.permission_id
                             WHERE rp.role_id = r.id) AS permissions
                FROM users u
                JOIN roles r ON r.tenant_id = u.tenant_id AND r.id = u.role_id
                JOIN tenants t ON t.id = u.tenant_id
                WHERE u.tenant_id = :tenantId AND u.id = :id AND u.status = 'ACTIVE'
                  AND u.session_version = :sessionVersion AND NOT u.password_change_required
                """).param("tenantId", session.tenantId()).param("id", session.id())
                .param("sessionVersion", session.sessionVersion())
                .query((row, index) -> new Manager(row.getBoolean("owner"),
                        Set.of((String[]) row.getArray("permissions").getArray())))
                .optional();
    }

    public Page<TenantAccountResponse> list(UUID tenantId, Pageable page) {
        List<TenantAccountResponse> items = jdbc.sql(ACCOUNT_SELECT + """
                AND u.status <> 'ARCHIVED'
                ORDER BY u.username, u.id
                LIMIT :size OFFSET :offset
                """).param("tenantId", tenantId).param("size", page.getPageSize())
                .param("offset", page.getOffset()).query(TenantAccountRepository::map).list();
        long total = jdbc.sql("""
                SELECT count(*) FROM users WHERE tenant_id = :tenantId AND status <> 'ARCHIVED'
                """).param("tenantId", tenantId).query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public Optional<TenantAccountResponse> find(UUID tenantId, UUID id) {
        return jdbc.sql(ACCOUNT_SELECT + "AND u.id = :id")
                .param("tenantId", tenantId).param("id", id).query(TenantAccountRepository::map).optional();
    }

    public UUID insert(UUID tenantId, UUID roleId, String username, String phoneNumber, String passwordHash) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO users (id, tenant_id, role_id, username, phone_number, password_hash,
                                   password_change_required)
                VALUES (:id, :tenantId, :roleId, :username, :phoneNumber, :passwordHash, true)
                """).param("id", id).param("tenantId", tenantId).param("roleId", roleId)
                .param("username", username).param("phoneNumber", phoneNumber, Types.VARCHAR)
                .param("passwordHash", passwordHash).update();
        return id;
    }

    public void assignRole(UUID tenantId, UUID id, UUID roleId, String phoneNumber) {
        jdbc.sql("""
                UPDATE users SET role_id = :roleId, phone_number = coalesce(:phoneNumber, phone_number)
                WHERE tenant_id = :tenantId AND id = :id
                """).param("id", id).param("tenantId", tenantId).param("roleId", roleId)
                .param("phoneNumber", phoneNumber, Types.VARCHAR).update();
    }

    public void updateStatus(UUID tenantId, UUID id, Status status) {
        jdbc.sql("""
                UPDATE users SET status = :status,
                                 session_version = session_version + CASE WHEN :status = 'ACTIVE' THEN 0 ELSE 1 END
                WHERE tenant_id = :tenantId AND id = :id
                """).param("id", id).param("tenantId", tenantId).param("status", status.name()).update();
    }

    public void resetPassword(UUID tenantId, UUID id, String passwordHash) {
        jdbc.sql("""
                UPDATE users SET password_hash = :passwordHash, password_change_required = true,
                                 session_version = session_version + 1
                WHERE tenant_id = :tenantId AND id = :id
                """).param("id", id).param("tenantId", tenantId).param("passwordHash", passwordHash).update();
    }

    private static TenantAccountResponse map(ResultSet row, int index) throws SQLException {
        return new TenantAccountResponse(row.getObject("id", UUID.class), row.getObject("tenant_id", UUID.class),
                row.getString("username"), row.getObject("role_id", UUID.class), row.getString("role_code"),
                Status.valueOf(row.getString("status")), row.getBoolean("owner"));
    }

    public record Manager(boolean owner, Set<String> permissions) {
    }
}
