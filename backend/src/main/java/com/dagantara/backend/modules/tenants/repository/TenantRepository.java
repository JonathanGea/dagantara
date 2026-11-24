package com.dagantara.backend.modules.tenants.repository;

import com.dagantara.backend.modules.tenants.dto.TenantResponse;
import com.dagantara.backend.modules.tenants.dto.TenantResponse.Status;
import com.dagantara.backend.modules.tenants.service.TenantLoginAddress.Address;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository {
    private static final String SELECT_TENANT = """
            SELECT t.id, t.name, t.status, (
                SELECT d.hostname || d.shop_path FROM tenant_domains d
                WHERE d.tenant_id = t.id ORDER BY d.hostname, d.shop_path LIMIT 1
            ) AS store_address
            FROM tenants t
            """;

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

    public Optional<UUID> findActiveTenantAt(String hostname, String shopPath) {
        return jdbc.sql("""
                SELECT t.id FROM tenant_domains d JOIN tenants t ON t.id = d.tenant_id
                WHERE d.hostname = :hostname AND d.shop_path = :shopPath AND t.status = 'ACTIVE'
                """).param("hostname", hostname).param("shopPath", shopPath).query(UUID.class).optional();
    }

    public boolean lockActiveTenantForLogin(UUID tenantId) {
        return jdbc.sql("""
                SELECT id FROM tenants WHERE id = :id AND status = 'ACTIVE' FOR SHARE
                """).param("id", tenantId).query(UUID.class).optional().isPresent();
    }

    public Page<TenantResponse> list(Pageable page) {
        var tenants = jdbc.sql(SELECT_TENANT + " ORDER BY t.name, t.id LIMIT :limit OFFSET :offset")
                .param("limit", page.getPageSize()).param("offset", page.getOffset())
                .query(TenantRepository::readTenant).list();
        long total = jdbc.sql("SELECT COUNT(*) FROM tenants").query(Long.class).single();
        return new PageImpl<>(tenants, page, total);
    }

    public Optional<TenantResponse> lockTenant(UUID tenantId) {
        return jdbc.sql(SELECT_TENANT + " WHERE t.id = :id FOR UPDATE OF t").param("id", tenantId)
                .query(TenantRepository::readTenant).optional();
    }

    public void updateStatus(UUID tenantId, Status status) {
        jdbc.sql("UPDATE tenants SET status = :status WHERE id = :id")
                .param("id", tenantId).param("status", status.name()).update();
    }

    private static TenantResponse readTenant(ResultSet row, int index) throws SQLException {
        return new TenantResponse(row.getObject("id", UUID.class), row.getString("name"),
                row.getString("store_address"), Status.valueOf(row.getString("status")));
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
