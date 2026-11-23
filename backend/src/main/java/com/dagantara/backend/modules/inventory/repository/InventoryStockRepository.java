package com.dagantara.backend.modules.inventory.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryStockRepository {
    private final JdbcClient jdbc;

    public InventoryStockRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID ensureDefaultWarehouse(UUID tenantId) {
        var existing = jdbc.sql("SELECT id FROM inventory_warehouses WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId).query(UUID.class).optional();
        if (existing.isPresent()) {
            return existing.get();
        }
        jdbc.sql("""
                INSERT INTO inventory_warehouses (id, tenant_id)
                VALUES (gen_random_uuid(), :tenantId) ON CONFLICT (tenant_id) DO NOTHING
                """).param("tenantId", tenantId).update();
        return jdbc.sql("SELECT id FROM inventory_warehouses WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId).query(UUID.class).single();
    }

    public boolean addReceiptQuantity(UUID tenantId, UUID warehouseId, UUID variantId, long quantity) {
        return jdbc.sql("""
                INSERT INTO inventory_stock_balances AS stock (tenant_id, warehouse_id, variant_id, quantity)
                VALUES (:tenantId, :warehouseId, :variantId, :quantity)
                ON CONFLICT (warehouse_id, variant_id) DO UPDATE
                SET quantity = stock.quantity + EXCLUDED.quantity
                WHERE stock.quantity <= :maxPrevious
                """).param("tenantId", tenantId).param("warehouseId", warehouseId).param("variantId", variantId)
                .param("quantity", quantity).param("maxPrevious", Long.MAX_VALUE - quantity).update() == 1;
    }

    /** Returns owned variants only, including zero for variants without a stock balance. */
    public Map<UUID, Long> quantities(UUID tenantId, Collection<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("""
                SELECT v.id, COALESCE(b.quantity, 0) AS quantity
                FROM product_variants v
                LEFT JOIN inventory_warehouses w ON w.tenant_id = v.tenant_id
                LEFT JOIN inventory_stock_balances b ON b.warehouse_id = w.id AND b.tenant_id = v.tenant_id
                    AND b.variant_id = v.id
                WHERE v.tenant_id = :tenantId AND v.id = ANY (:variantIds)
                """).param("tenantId", tenantId)
                .param("variantIds", new SqlArrayValue("uuid", variantIds.toArray())).query(rows -> {
                    Map<UUID, Long> quantities = new HashMap<>();
                    while (rows.next()) {
                        quantities.put(rows.getObject("id", UUID.class), rows.getLong("quantity"));
                    }
                    return Map.copyOf(quantities);
                });
    }
}
