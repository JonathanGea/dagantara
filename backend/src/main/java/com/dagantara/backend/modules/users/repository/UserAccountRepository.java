package com.dagantara.backend.modules.users.repository;

import com.dagantara.backend.modules.users.entity.UserAccount;
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

    Optional<UserAccount> findByIdAndTenantId(UUID id, UUID tenantId);
}
