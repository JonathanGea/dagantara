package com.dagantara.backend.modules.tenants.repository;

import com.dagantara.backend.modules.tenants.dto.TenantApplicationResponse;
import com.dagantara.backend.modules.tenants.dto.TenantApplicationResponse.Status;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantApplicationRepository {
    private final JdbcClient jdbc;

    public TenantApplicationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID insert(String storeName, String phoneNumber, String proposedDomain) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO tenant_applications (id, store_name, phone_number, proposed_domain)
                VALUES (:id, :storeName, :phoneNumber, :proposedDomain)
                """).param("id", id).param("storeName", storeName)
                .param("phoneNumber", phoneNumber).param("proposedDomain", proposedDomain).update();
        return id;
    }

    public Page<TenantApplicationResponse> list(Pageable page) {
        List<TenantApplicationResponse> items = jdbc.sql("""
                SELECT * FROM tenant_applications
                ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset
                """).param("limit", page.getPageSize()).param("offset", page.getOffset())
                .query(TenantApplicationRepository::read).list();
        long total = jdbc.sql("SELECT count(*) FROM tenant_applications").query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public Optional<TenantApplicationResponse> find(UUID id) {
        return jdbc.sql("SELECT * FROM tenant_applications WHERE id = :id")
                .param("id", id).query(TenantApplicationRepository::read).optional();
    }

    public Optional<TenantApplicationResponse> lock(UUID id) {
        return jdbc.sql("SELECT * FROM tenant_applications WHERE id = :id FOR UPDATE")
                .param("id", id).query(TenantApplicationRepository::read).optional();
    }

    public TenantApplicationResponse updateProposal(UUID id, String proposedDomain) {
        return jdbc.sql("""
                UPDATE tenant_applications SET proposed_domain = :proposedDomain WHERE id = :id
                RETURNING *
                """).param("id", id).param("proposedDomain", proposedDomain)
                .query(TenantApplicationRepository::read).single();
    }

    public TenantApplicationResponse changeStatus(UUID id, Status status, UUID tenantId) {
        return jdbc.sql("""
                UPDATE tenant_applications SET status = :status, tenant_id = :tenantId WHERE id = :id
                RETURNING *
                """).param("id", id).param("status", status.name()).param("tenantId", tenantId)
                .query(TenantApplicationRepository::read).single();
    }

    private static TenantApplicationResponse read(ResultSet row, int rowNumber) throws SQLException {
        return new TenantApplicationResponse(row.getObject("id", UUID.class), row.getString("store_name"),
                row.getString("phone_number"), row.getString("proposed_domain"),
                Status.valueOf(row.getString("status")), row.getObject("tenant_id", UUID.class));
    }
}
