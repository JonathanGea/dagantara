package com.dagantara.backend.modules.inventory.repository;

import com.dagantara.backend.modules.inventory.dto.InventoryResponses.Correction;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.CorrectionLine;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryCorrectionRepository {
    private static final String SELECT_CORRECTIONS = """
            SELECT c.id, c.receipt_id, c.corrected_by, c.created_at, c.reason, l.receipt_line_id,
                   l.previous_quantity, l.quantity, l.delta
            FROM inventory_corrections c
            JOIN inventory_correction_lines l ON l.tenant_id = c.tenant_id AND l.receipt_id = c.receipt_id
                AND l.correction_id = c.id
            WHERE c.tenant_id = :tenantId
            """;
    private final JdbcClient jdbc;

    public InventoryCorrectionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean claimKey(UUID tenantId, UUID accountId, String key, byte[] hash, UUID correctionId) {
        return jdbc.sql("""
                INSERT INTO inventory_correction_keys (tenant_id, account_id, idempotency_key, request_hash, correction_id)
                VALUES (:tenantId, :accountId, :key, :hash, :correctionId)
                ON CONFLICT (tenant_id, account_id, idempotency_key) DO NOTHING
                """).param("tenantId", tenantId).param("accountId", accountId)
                .param("key", key).param("hash", hash).param("correctionId", correctionId).update() == 1;
    }

    public CorrectionKey getKey(UUID tenantId, UUID accountId, String key) {
        return jdbc.sql("""
                SELECT request_hash, correction_id FROM inventory_correction_keys
                WHERE tenant_id = :tenantId AND account_id = :accountId AND idempotency_key = :key
                """).param("tenantId", tenantId).param("accountId", accountId).param("key", key)
                .query((row, index) -> new CorrectionKey(row.getBytes("request_hash"),
                        row.getObject("correction_id", UUID.class))).single();
    }

    public void insertCorrection(UUID tenantId, UUID warehouseId, UUID receiptId, UUID accountId, UUID id,
                                 String reason) {
        jdbc.sql("""
                INSERT INTO inventory_corrections (id, tenant_id, receipt_id, warehouse_id, corrected_by, reason)
                VALUES (:id, :tenantId, :receiptId, :warehouseId, :accountId, :reason)
                """).param("id", id).param("tenantId", tenantId).param("receiptId", receiptId)
                .param("warehouseId", warehouseId).param("accountId", accountId).param("reason", reason).update();
    }

    public void insertLine(UUID tenantId, UUID receiptId, UUID correctionId, CorrectionLine line) {
        jdbc.sql("""
                INSERT INTO inventory_correction_lines
                    (tenant_id, receipt_id, correction_id, receipt_line_id, previous_quantity, quantity)
                VALUES (:tenantId, :receiptId, :correctionId, :lineId, :previous, :quantity)
                """).param("tenantId", tenantId).param("receiptId", receiptId).param("correctionId", correctionId)
                .param("lineId", line.receiptLineId()).param("previous", line.previousQuantity())
                .param("quantity", line.quantity()).update();
    }

    public Map<UUID, Long> effectiveQuantities(UUID tenantId, UUID receiptId, Set<UUID> lineIds) {
        return jdbc.sql("""
                SELECT r.id, r.quantity + COALESCE(sum(c.delta), 0) AS effective_quantity
                FROM inventory_receipt_lines r
                LEFT JOIN inventory_correction_lines c ON c.tenant_id = r.tenant_id AND c.receipt_id = r.receipt_id
                    AND c.receipt_line_id = r.id
                WHERE r.tenant_id = :tenantId AND r.receipt_id = :receiptId AND r.id IN (:lineIds)
                GROUP BY r.id
                """).param("tenantId", tenantId).param("receiptId", receiptId).param("lineIds", lineIds)
                .query(rows -> {
                    Map<UUID, Long> quantities = new LinkedHashMap<>();
                    while (rows.next()) {
                        quantities.put(rows.getObject("id", UUID.class), rows.getLong("effective_quantity"));
                    }
                    return quantities;
                });
    }

    public Correction getCorrection(UUID tenantId, UUID correctionId) {
        return readCorrections(jdbc.sql(SELECT_CORRECTIONS + " AND c.id = :id ORDER BY l.receipt_line_id")
                .param("tenantId", tenantId).param("id", correctionId)).getFirst();
    }

    public List<Correction> listCorrections(UUID tenantId, UUID receiptId) {
        return readCorrections(jdbc.sql(SELECT_CORRECTIONS
                + " AND c.receipt_id = :receiptId ORDER BY c.correction_order, l.receipt_line_id")
                .param("tenantId", tenantId).param("receiptId", receiptId));
    }

    private static List<Correction> readCorrections(JdbcClient.StatementSpec query) {
        return query.query(rows -> {
            Map<UUID, Correction> corrections = new LinkedHashMap<>();
            while (rows.next()) {
                UUID id = rows.getObject("id", UUID.class);
                Correction correction = corrections.get(id);
                if (correction == null) {
                    correction = new Correction(id, rows.getObject("receipt_id", UUID.class),
                            rows.getObject("corrected_by", UUID.class), rows.getObject("created_at", OffsetDateTime.class),
                            rows.getString("reason"), new ArrayList<>());
                    corrections.put(id, correction);
                }
                correction.lines().add(new CorrectionLine(rows.getObject("receipt_line_id", UUID.class),
                        rows.getLong("previous_quantity"), rows.getLong("quantity"), rows.getLong("delta")));
            }
            return corrections.values().stream().map(correction -> new Correction(
                    correction.id(), correction.receiptId(), correction.correctedBy(), correction.createdAt(),
                    correction.reason(), List.copyOf(correction.lines()))).toList();
        });
    }

    public record CorrectionKey(byte[] requestHash, UUID correctionId) {
    }
}
