package com.dagantara.backend.modules.catalog.repository;

import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.ProductCard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StorefrontRepository {
    private final JdbcClient jdbc;

    public StorefrontRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Page<ProductCard> listPublished(UUID tenantId, Pageable page) {
        List<ProductCard> items = jdbc.sql("""
                SELECT p.id, p.name, p.price_idr, photo.url
                FROM products p
                LEFT JOIN LATERAL (
                    SELECT m.url FROM product_media m
                    WHERE m.tenant_id = p.tenant_id AND m.product_id = p.id
                    ORDER BY m.created_at, m.id LIMIT 1
                ) photo ON true
                WHERE p.tenant_id = :tenantId AND p.status = 'PUBLISHED'
                ORDER BY p.name, p.id LIMIT :size OFFSET :offset
                """).param("tenantId", tenantId).param("size", page.getPageSize()).param("offset", page.getOffset())
                .query((row, index) -> new ProductCard(row.getObject("id", UUID.class), row.getString("name"),
                        row.getLong("price_idr"), row.getString("url"))).list();
        long total = jdbc.sql("SELECT count(*) FROM products WHERE tenant_id = :tenantId AND status = 'PUBLISHED'")
                .param("tenantId", tenantId).query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }
}
