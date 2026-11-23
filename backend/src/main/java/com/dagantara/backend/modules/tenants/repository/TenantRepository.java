package com.dagantara.backend.modules.tenants.repository;

import com.dagantara.backend.modules.tenants.service.TenantLoginAddress.Address;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository {
    private final JdbcClient jdbc;

    public TenantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> findTenantAt(String hostname, String shopPath) {
        return jdbc.sql("""
                SELECT tenant_id FROM tenant_domains
                WHERE hostname = :hostname AND shop_path = :shopPath
                """).param("hostname", hostname).param("shopPath", shopPath).query(UUID.class).optional();
    }

    public void insertTenant(UUID id, String name, UUID ownerId) {
        jdbc.sql("INSERT INTO tenants (id, name, owner_user_id) VALUES (:id, :name, :ownerId)")
                .param("id", id).param("name", name).param("ownerId", ownerId).update();
    }

    public boolean reserveAddress(UUID tenantId, Address address) {
        return jdbc.sql("""
                INSERT INTO tenant_domains (hostname, shop_path, tenant_id)
                VALUES (:hostname, :shopPath, :tenantId)
                ON CONFLICT (hostname, shop_path) DO NOTHING
                """).param("hostname", address.hostname()).param("shopPath", address.shopPath())
                .param("tenantId", tenantId).update() == 1;
    }
}
