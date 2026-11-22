package com.dagantara.backend.modules.users.repository;

import com.dagantara.backend.modules.users.dto.AccountSession;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
    private static final String SESSION_COLUMNS =
            "u.id, u.tenant_id, u.session_version, u.password_change_required";
    private static final String ACCOUNT_FROM = """
            FROM users u JOIN roles r ON r.id = u.role_id
            WHERE r.tenant_id IS NOT DISTINCT FROM u.tenant_id
            """;

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID lockPlatformAdminRole() {
        return jdbc.sql("""
                SELECT id FROM roles
                WHERE tenant_id IS NULL AND code = 'SUPERADMIN'
                FOR UPDATE
                """).query(UUID.class).single();
    }

    public void insertInitialAdminIfAbsent(UUID roleId, String username, String passwordHash) {
        jdbc.sql("""
                INSERT INTO users (id, role_id, username, password_hash, is_initial_platform_admin)
                SELECT :id, :roleId, :username, :passwordHash, true
                WHERE NOT EXISTS (SELECT 1 FROM users WHERE is_initial_platform_admin)
                """).param("id", UUID.randomUUID()).param("roleId", roleId)
                .param("username", username).param("passwordHash", passwordHash).update();
    }

    public void insertTenantOwner(UUID id, UUID tenantId, UUID roleId, String username,
                                  String phoneNumber, String passwordHash) {
        jdbc.sql("""
                INSERT INTO users (id, tenant_id, role_id, username, phone_number, password_hash,
                                   password_change_required)
                VALUES (:id, :tenantId, :roleId, :username, :phoneNumber, :passwordHash, true)
                """).param("id", id).param("tenantId", tenantId).param("roleId", roleId)
                .param("username", username).param("phoneNumber", phoneNumber)
                .param("passwordHash", passwordHash).update();
    }

    public Optional<AccountCredentials> findLogin(String username, UUID tenantId) {
        return jdbc.sql("SELECT " + SESSION_COLUMNS + ", u.password_hash " + ACCOUNT_FROM + """
                 AND u.tenant_id IS NOT DISTINCT FROM CAST(:tenantId AS uuid) AND u.username = :username
                """).param("username", username).param("tenantId", tenantId)
                .query((row, index) -> new AccountCredentials(readSession(row), row.getString("password_hash")))
                .optional();
    }

    public Optional<AccountSession> findSession(UUID id) {
        return jdbc.sql("SELECT " + SESSION_COLUMNS + " " + ACCOUNT_FROM + " AND u.id = :id").param("id", id)
                .query((row, index) -> readSession(row)).optional();
    }

    public Optional<String> lockTemporaryPassword(AccountSession session) {
        return jdbc.sql("""
                SELECT password_hash FROM users
                WHERE id = :id AND session_version = :sessionVersion AND password_change_required
                FOR UPDATE
                """).param("id", session.id()).param("sessionVersion", session.sessionVersion())
                .query(String.class).optional();
    }

    public void replaceTemporaryPassword(UUID id, String passwordHash) {
        jdbc.sql("""
                UPDATE users SET password_hash = :passwordHash, password_change_required = false,
                                 session_version = session_version + 1
                WHERE id = :id
                """).param("id", id).param("passwordHash", passwordHash).update();
    }

    private static AccountSession readSession(ResultSet row) throws SQLException {
        return new AccountSession(row.getObject("id", UUID.class), row.getObject("tenant_id", UUID.class),
                row.getInt("session_version"), row.getBoolean("password_change_required"));
    }

    public record AccountCredentials(AccountSession session, String passwordHash) {
        @Override
        public String toString() {
            return "AccountCredentials[session=" + session + "]";
        }
    }
}
