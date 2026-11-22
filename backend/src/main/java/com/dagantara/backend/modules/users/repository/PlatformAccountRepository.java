package com.dagantara.backend.modules.users.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformAccountRepository {
    private final JdbcClient jdbc;

    public PlatformAccountRepository(JdbcClient jdbc) {
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
                """)
                .param("id", UUID.randomUUID())
                .param("roleId", roleId)
                .param("username", username)
                .param("passwordHash", passwordHash)
                .update();
    }

    public Optional<PlatformLogin> findLoginByUsername(String username) {
        return jdbc.sql("""
                SELECT u.id, u.password_hash
                FROM users u JOIN roles r ON r.id = u.role_id
                WHERE u.tenant_id IS NULL AND r.tenant_id IS NULL AND u.username = :username
                """)
                .param("username", username)
                .query((row, index) -> new PlatformLogin(
                        row.getObject("id", UUID.class), row.getString("password_hash")))
                .optional();
    }

    public static final class PlatformLogin {
        private final UUID id;
        private final String passwordHash;

        private PlatformLogin(UUID id, String passwordHash) {
            this.id = id;
            this.passwordHash = passwordHash;
        }

        public UUID id() {
            return id;
        }

        public String passwordHash() {
            return passwordHash;
        }
    }
}
