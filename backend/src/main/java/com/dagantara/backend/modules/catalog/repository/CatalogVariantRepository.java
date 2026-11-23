package com.dagantara.backend.modules.catalog.repository;

import com.dagantara.backend.modules.catalog.dto.CatalogRequests.SkuMode;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Variant;
import com.dagantara.backend.modules.catalog.service.SkuCodes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogVariantRepository {
    private static final String SELECT = """
            SELECT id, product_id, sku, sku_mode, combination_key, first_stock_recorded_at IS NOT NULL AS stock_recorded
            FROM product_variants WHERE tenant_id = :tenantId
            """;

    private final JdbcClient jdbc;

    public CatalogVariantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<VariantRow> listForProduct(UUID tenantId, UUID productId) {
        return jdbc.sql(SELECT + " AND product_id = :productId ORDER BY id")
                .param("tenantId", tenantId).param("productId", productId)
                .query(CatalogVariantRepository::readVariant).list();
    }

    public List<VariantRow> findByIds(UUID tenantId, Collection<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(SELECT + " AND id IN (:ids) ORDER BY id")
                .param("tenantId", tenantId).param("ids", variantIds)
                .query(CatalogVariantRepository::readVariant).list();
    }

    public Optional<VariantRow> findBySku(UUID tenantId, String sku) {
        return jdbc.sql(SELECT + " AND sku = :sku").param("tenantId", tenantId)
                .param("sku", SkuCodes.normalizeSku(sku)).query(CatalogVariantRepository::readVariant).optional();
    }

    /** Receipt writers lock every SKU together in UUID order before writing stock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<VariantRow> lockBySkus(UUID tenantId, Collection<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        List<String> normalized = skus.stream().map(SkuCodes::normalizeSku).distinct().toList();
        return jdbc.sql(SELECT + " AND sku IN (:skus) ORDER BY id FOR UPDATE")
                .param("tenantId", tenantId).param("skus", normalized)
                .query(CatalogVariantRepository::readVariant).list();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<VariantRow> lockForProduct(UUID tenantId, UUID productId, UUID variantId) {
        return jdbc.sql(SELECT + " AND product_id = :productId AND id = :id FOR UPDATE")
                .param("tenantId", tenantId).param("productId", productId).param("id", variantId)
                .query(CatalogVariantRepository::readVariant).optional();
    }

    /** Mark the already locked variants in the same transaction as the receipt and stock balance. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markStockRecorded(UUID tenantId, Collection<UUID> lockedVariantIds) {
        if (lockedVariantIds.isEmpty()) {
            return;
        }
        Set<UUID> ids = Set.copyOf(lockedVariantIds);
        int updated = jdbc.sql("""
                UPDATE product_variants
                SET first_stock_recorded_at = COALESCE(first_stock_recorded_at, CURRENT_TIMESTAMP)
                WHERE tenant_id = :tenantId AND id IN (:ids)
                """).param("tenantId", tenantId).param("ids", ids).update();
        if (updated != ids.size()) {
            throw new IllegalStateException("Stock recording requires existing variants in the locked tenant");
        }
    }

    public Variant insert(UUID tenantId, UUID productId, SkuMode mode, String sku,
                          List<CatalogRepository.SelectedValue> values) {
        UUID id = UUID.randomUUID();
        List<UUID> combination = values.stream().map(value -> value.value().id()).toList();
        jdbc.sql("""
                INSERT INTO product_variants (id, tenant_id, product_id, sku_mode, sku, combination_key)
                VALUES (:id, :tenantId, :productId, :mode, :sku, :combination)
                """).param("id", id).param("tenantId", tenantId).param("productId", productId)
                .param("mode", mode.name()).param("sku", sku)
                .param("combination", new SqlArrayValue("uuid", combination.toArray())).update();

        for (var value : values) {
            jdbc.sql("""
                    INSERT INTO product_variant_option_values (product_id, variant_id, option_id, option_value_id)
                    VALUES (:productId, :id, :optionId, :valueId)
                    """).param("productId", productId).param("id", id).param("optionId", value.optionId())
                    .param("valueId", value.value().id()).update();
        }
        return new Variant(id, combination, sku, mode);
    }

    public boolean changeSku(UUID tenantId, UUID variantId, SkuMode mode, String sku) {
        return jdbc.sql("""
                UPDATE product_variants SET sku_mode = :mode, sku = :sku
                WHERE tenant_id = :tenantId AND id = :id AND first_stock_recorded_at IS NULL
                """).param("tenantId", tenantId).param("id", variantId).param("mode", mode.name()).param("sku", sku)
                .update() == 1;
    }

    private static VariantRow readVariant(ResultSet row, int index) throws SQLException {
        var combination = row.getArray("combination_key");
        try {
            return new VariantRow(row.getObject("id", UUID.class), row.getObject("product_id", UUID.class),
                    row.getString("sku"), SkuMode.valueOf(row.getString("sku_mode")),
                    List.of((UUID[]) combination.getArray()), row.getBoolean("stock_recorded"));
        } finally {
            combination.free();
        }
    }

    public record VariantRow(UUID id, UUID productId, String sku, SkuMode skuMode, List<UUID> optionValueIds,
                             boolean stockRecorded) {
        public Variant toResponse() {
            return new Variant(id, optionValueIds, sku, skuMode);
        }
    }
}
