package com.dagantara.backend.modules.users.repository;

import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.dto.PlatformRoleDtos.Role;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.Status;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.User;
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
public class PlatformOperatorRepository {
    private static final String PLATFORM_USERS = """
            FROM users u JOIN roles r ON r.id = u.role_id
            WHERE u.tenant_id IS NULL AND r.tenant_id IS NULL
            """;
    private static final String SELECT_USER = """
            SELECT u.id, u.username, u.role_id, r.code, u.status, u.is_initial_platform_admin
            """ + PLATFORM_USERS;
    private static final String UNARCHIVED_USERS = " AND u.status <> 'ARCHIVED' ";
    private static final String SELECT_ROLE = """
            SELECT r.id, r.code, r.name, ARRAY(
                SELECT p.code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = r.id
            ) AS permission_codes
            FROM roles r WHERE r.tenant_id IS NULL
            """;

    private final JdbcClient jdbc;

    public PlatformOperatorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findActor(AccountSession session) {
        return jdbc.sql(SELECT_USER + """
                AND u.id = :id AND u.status = 'ACTIVE' AND u.session_version = :version
                AND NOT u.password_change_required
                """).param("id", session.id()).param("version", session.sessionVersion())
                .query(PlatformOperatorRepository::readUser).optional();
    }

    public Page<User> listUsers(Pageable page) {
        List<User> items = jdbc.sql(SELECT_USER + UNARCHIVED_USERS + """
                ORDER BY u.username, u.id LIMIT :limit OFFSET :offset
                """).param("limit", page.getPageSize()).param("offset", page.getOffset())
                .query(PlatformOperatorRepository::readUser).list();
        long total = jdbc.sql("SELECT count(*) " + PLATFORM_USERS + UNARCHIVED_USERS).query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public Optional<User> findUser(UUID id) {
        return jdbc.sql(SELECT_USER + " AND u.id = :id").param("id", id)
                .query(PlatformOperatorRepository::readUser).optional();
    }

    public UUID createUser(String username, UUID roleId, String passwordHash) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO users (id, username, role_id, password_hash, password_change_required)
                VALUES (:id, :username, :roleId, :passwordHash, true)
                """).param("id", id).param("username", username).param("roleId", roleId)
                .param("passwordHash", passwordHash).update();
        return id;
    }

    public void assignRole(UUID id, UUID roleId) {
        jdbc.sql("UPDATE users SET role_id = :roleId WHERE id = :id AND tenant_id IS NULL")
                .param("id", id).param("roleId", roleId).update();
    }

    public void resetPassword(UUID id, String passwordHash) {
        jdbc.sql("""
                UPDATE users SET password_hash = :passwordHash, password_change_required = true,
                                 session_version = session_version + 1
                WHERE id = :id AND tenant_id IS NULL
                """).param("id", id).param("passwordHash", passwordHash).update();
    }

    public void changeStatus(UUID id, Status status) {
        jdbc.sql("""
                UPDATE users SET status = :status, session_version = session_version + 1
                WHERE id = :id AND tenant_id IS NULL
                """).param("id", id).param("status", status.name()).update();
    }

    public List<Role> listRoles() {
        return jdbc.sql(SELECT_ROLE + " ORDER BY r.code, r.id")
                .query(PlatformOperatorRepository::readRole).list();
    }

    public Optional<Role> findRole(UUID id) {
        return jdbc.sql(SELECT_ROLE + " AND r.id = :id").param("id", id)
                .query(PlatformOperatorRepository::readRole).optional();
    }

    public Role createRole(String code, String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO roles (id, code, name) VALUES (:id, :code, :name)")
                .param("id", id).param("code", code).param("name", name).update();
        return new Role(id, code, name, Set.of());
    }

    public Set<String> permissionCatalog() {
        return Set.copyOf(jdbc.sql("SELECT code FROM permissions").query(String.class).list());
    }

    public void replacePermissions(UUID roleId, Set<String> codes) {
        jdbc.sql("DELETE FROM role_permissions WHERE role_id = :roleId").param("roleId", roleId).update();
        if (!codes.isEmpty()) {
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT :roleId, id FROM permissions WHERE code IN (:codes)
                    """).param("roleId", roleId).param("codes", codes).update();
        }
    }

    private static User readUser(ResultSet row, int index) throws SQLException {
        return new User(row.getObject("id", UUID.class), row.getString("username"),
                row.getObject("role_id", UUID.class), row.getString("code"),
                Status.valueOf(row.getString("status")), row.getBoolean("is_initial_platform_admin"));
    }

    private static Role readRole(ResultSet row, int index) throws SQLException {
        return new Role(row.getObject("id", UUID.class), row.getString("code"), row.getString("name"),
                Set.of((String[]) row.getArray("permission_codes").getArray()));
    }
}
