package com.dagantara.backend.modules.helpcenter.repository;

import com.dagantara.backend.modules.helpcenter.dto.RecoveryTarget;
import com.dagantara.backend.modules.helpcenter.dto.VerifiedResetRequest.VerificationMethod;
import com.dagantara.backend.modules.tenants.service.TenantLoginAddress.Address;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RecoveryRepository {
    private static final String RECOVERABLE_ADMINS = """
            FROM users u
            JOIN roles r ON r.tenant_id = u.tenant_id AND r.id = u.role_id
            WHERE r.code = 'TENANT_ADMIN' AND u.status = 'ACTIVE'
              AND u.phone_number IS NOT NULL AND btrim(u.phone_number) <> ''
            """;

    private final JdbcClient jdbc;

    public RecoveryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RecoveryTarget> findTarget(Address address, String username) {
        return jdbc.sql("SELECT u.tenant_id, u.id, u.phone_number " + RECOVERABLE_ADMINS + """
                AND u.username = :username
                AND u.tenant_id = (SELECT tenant_id FROM tenant_domains
                                   WHERE hostname = :hostname AND shop_path = :shopPath)
                """).param("username", username).param("hostname", address.hostname())
                .param("shopPath", address.shopPath())
                .query((row, index) -> new RecoveryTarget(row.getObject("tenant_id", UUID.class),
                        row.getObject("id", UUID.class), row.getString("phone_number")))
                .optional();
    }

    public boolean isRecoverableAdmin(UUID tenantId, UUID accountId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 " + RECOVERABLE_ADMINS + """
                AND u.tenant_id = :tenantId AND u.id = :accountId)
                """).param("tenantId", tenantId).param("accountId", accountId).query(Boolean.class).single();
    }

    public void recordReset(UUID operatorId, UUID tenantId, UUID accountId, VerificationMethod method) {
        jdbc.sql("""
                INSERT INTO tenant_admin_recovery_audit
                    (id, operator_id, tenant_id, target_user_id, verification_method)
                VALUES (:id, :operatorId, :tenantId, :accountId, :method)
                """).param("id", UUID.randomUUID()).param("operatorId", operatorId)
                .param("tenantId", tenantId).param("accountId", accountId).param("method", method.name())
                .update();
    }
}
