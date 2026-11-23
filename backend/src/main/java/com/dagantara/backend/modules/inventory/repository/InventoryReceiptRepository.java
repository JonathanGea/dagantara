package com.dagantara.backend.modules.inventory.repository;

import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptLine;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryReceiptRepository {
    private final JdbcClient jdbc;

    public InventoryReceiptRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean claimKey(UUID tenantId, UUID accountId, String key, byte[] hash, UUID receiptId) {
        return jdbc.sql("""
                INSERT INTO inventory_receipt_keys (tenant_id, account_id, idempotency_key, request_hash, receipt_id)
                VALUES (:tenantId, :accountId, :key, :hash, :receiptId)
                ON CONFLICT (tenant_id, account_id, idempotency_key) DO NOTHING
                """).param("tenantId", tenantId).param("accountId", accountId)
                .param("key", key).param("hash", hash).param("receiptId", receiptId).update() == 1;
    }

    public ReceiptKey getKey(UUID tenantId, UUID accountId, String key) {
        return jdbc.sql("""
                SELECT request_hash, receipt_id FROM inventory_receipt_keys
                WHERE tenant_id = :tenantId AND account_id = :accountId AND idempotency_key = :key
                """).param("tenantId", tenantId).param("accountId", accountId).param("key", key)
                .query((row, index) -> new ReceiptKey(row.getBytes("request_hash"),
                        row.getObject("receipt_id", UUID.class))).single();
    }

    public ReceiptSummary insertReceipt(UUID tenantId, UUID warehouseId, UUID accountId, UUID id) {
        return jdbc.sql("""
                INSERT INTO inventory_receipts (id, tenant_id, warehouse_id, received_by)
                VALUES (:id, :tenantId, :warehouseId, :accountId)
                RETURNING id, warehouse_id, received_by, created_at
                """).param("id", id).param("tenantId", tenantId).param("warehouseId", warehouseId)
                .param("accountId", accountId).query(InventoryReceiptRepository::readSummary).single();
    }

    public void insertLine(UUID tenantId, UUID receiptId, ReceiptLine line) {
        jdbc.sql("""
                INSERT INTO inventory_receipt_lines (id, tenant_id, receipt_id, variant_id, sku, quantity)
                VALUES (:id, :tenantId, :receiptId, :variantId, :sku, :quantity)
                """).param("id", line.id()).param("tenantId", tenantId).param("receiptId", receiptId)
                .param("variantId", line.variantId()).param("sku", line.sku()).param("quantity", line.quantity())
                .update();
    }

    public Page<ReceiptSummary> listReceipts(UUID tenantId, Pageable page) {
        long total = jdbc.sql("SELECT count(*) FROM inventory_receipts WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId).query(Long.class).single();
        List<ReceiptSummary> summaries = jdbc.sql("""
                SELECT id, warehouse_id, received_by, created_at FROM inventory_receipts
                WHERE tenant_id = :tenantId
                ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset
                """).param("tenantId", tenantId).param("limit", page.getPageSize()).param("offset", page.getOffset())
                .query(InventoryReceiptRepository::readSummary).list();
        return new PageImpl<>(summaries, page, total);
    }

    public Optional<ReceiptSummary> findReceipt(UUID tenantId, UUID id) {
        return jdbc.sql("""
                SELECT id, warehouse_id, received_by, created_at FROM inventory_receipts
                WHERE tenant_id = :tenantId AND id = :id
                """).param("tenantId", tenantId).param("id", id)
                .query(InventoryReceiptRepository::readSummary).optional();
    }

    public List<ReceiptLine> listLines(UUID tenantId, UUID receiptId) {
        return jdbc.sql("""
                SELECT id, variant_id, sku, quantity FROM inventory_receipt_lines
                WHERE tenant_id = :tenantId AND receipt_id = :receiptId ORDER BY variant_id
                """).param("tenantId", tenantId).param("receiptId", receiptId)
                .query((row, index) -> new ReceiptLine(row.getObject("id", UUID.class),
                        row.getObject("variant_id", UUID.class), row.getString("sku"), row.getLong("quantity")))
                .list();
    }

    private static ReceiptSummary readSummary(ResultSet row, int index) throws SQLException {
        return new ReceiptSummary(row.getObject("id", UUID.class), row.getObject("warehouse_id", UUID.class),
                row.getObject("received_by", UUID.class), row.getObject("created_at", OffsetDateTime.class));
    }

    public record ReceiptKey(byte[] requestHash, UUID receiptId) {
    }
}
