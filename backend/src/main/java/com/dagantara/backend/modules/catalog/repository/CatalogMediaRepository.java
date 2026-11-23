package com.dagantara.backend.modules.catalog.repository;

import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Media;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogMediaRepository {
    private final JdbcClient jdbc;

    public CatalogMediaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Media> insert(UUID tenantId, UUID productId, String url) {
        return jdbc.sql("""
                INSERT INTO product_media (id, tenant_id, product_id, url)
                SELECT :id, tenant_id, id, :url FROM products WHERE tenant_id = :tenantId AND id = :productId
                RETURNING id
                """).param("id", UUID.randomUUID()).param("tenantId", tenantId).param("productId", productId)
                .param("url", url).query(UUID.class).optional().map(id -> new Media(id, url, List.of()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<String> lockUrl(UUID tenantId, UUID productId, UUID mediaId) {
        return jdbc.sql("""
                SELECT url FROM product_media WHERE tenant_id = :tenantId AND product_id = :productId AND id = :mediaId
                FOR UPDATE
                """).param("tenantId", tenantId).param("productId", productId).param("mediaId", mediaId)
                .query(String.class).optional();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> replaceOptionValues(UUID tenantId, UUID productId, UUID mediaId, Set<UUID> valueIds) {
        jdbc.sql("""
                DELETE FROM media_option_values mv USING product_media m
                WHERE m.tenant_id = :tenantId AND m.product_id = :productId AND m.id = :mediaId
                  AND mv.product_id = m.product_id AND mv.media_id = m.id
                """).param("tenantId", tenantId).param("productId", productId).param("mediaId", mediaId).update();
        if (valueIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                WITH inserted AS (
                    INSERT INTO media_option_values (product_id, media_id, option_value_id)
                    SELECT m.product_id, m.id, v.id FROM product_media m
                    JOIN product_option_values v ON v.product_id = m.product_id
                    WHERE m.tenant_id = :tenantId AND m.product_id = :productId AND m.id = :mediaId
                      AND v.id IN (:valueIds)
                    RETURNING option_value_id
                )
                SELECT option_value_id FROM inserted ORDER BY option_value_id
                """).param("tenantId", tenantId).param("productId", productId).param("mediaId", mediaId)
                .param("valueIds", valueIds).query(UUID.class).list();
    }

    public List<Media> listForProduct(UUID tenantId, UUID productId) {
        return jdbc.sql("""
                SELECT m.id, m.url, ARRAY(
                    SELECT mv.option_value_id FROM media_option_values mv
                    WHERE mv.media_id = m.id AND mv.product_id = m.product_id
                    ORDER BY mv.option_value_id
                ) AS option_value_ids
                FROM product_media m
                WHERE m.tenant_id = :tenantId AND m.product_id = :productId
                ORDER BY m.created_at, m.id
                """).param("tenantId", tenantId).param("productId", productId)
                .query((row, index) -> new Media(row.getObject("id", UUID.class), row.getString("url"),
                        List.of((UUID[]) row.getArray("option_value_ids").getArray()))).list();
    }
}
