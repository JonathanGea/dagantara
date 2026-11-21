package com.dagantara.backend.modules.users;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsernameAndTenantIdIsNull(String username);

    @Query(value = """
            SELECT u.* FROM users u
            JOIN tenants t ON t.id = u.tenant_id
            WHERE t.code = :tenantCode AND u.username = :username
            """, nativeQuery = true)
    Optional<UserAccount> findForTenantLogin(String username, String tenantCode);

    @Query(value = """
            SELECT u.* FROM users u
            JOIN roles r ON r.id = u.role_id
            JOIN role_permissions rp ON rp.role_id = r.id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE u.id = :userId AND u.status = 'ACTIVE' AND p.code = 'users.create'
              AND r.tenant_id IS NOT DISTINCT FROM u.tenant_id AND r.scope = u.account_type
            """, nativeQuery = true)
    Optional<UserAccount> findCreator(UUID userId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM roles r
                WHERE r.id = :roleId
                  AND r.tenant_id IS NOT DISTINCT FROM CAST(:tenantId AS uuid)
                  AND r.scope = :accountType
            )
            """, nativeQuery = true)
    boolean isAssignableOnCreate(UUID roleId, UUID tenantId, String accountType);
}
