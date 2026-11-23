package com.dagantara.backend.modules.inventory.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class InventoryResponses {
    private InventoryResponses() {
    }

    public record Receipt(UUID id, UUID warehouseId, UUID receivedBy, OffsetDateTime createdAt,
                          List<ReceiptLine> lines) {
    }

    public record ReceiptLine(UUID id, UUID variantId, String sku, long quantity) {
    }

    public record ReceiptSummary(UUID id, UUID warehouseId, UUID receivedBy, OffsetDateTime createdAt) {
    }

    public record ReceiptDetail(UUID id, UUID warehouseId, UUID receivedBy, OffsetDateTime createdAt,
                                List<ReceiptDetailLine> lines, List<Correction> corrections) {
    }

    public record ReceiptDetailLine(UUID id, UUID variantId, String sku, long originalQuantity,
                                    long effectiveQuantity) {
    }

    public record Correction(UUID id, UUID correctedBy, OffsetDateTime createdAt, String reason,
                             List<CorrectionLine> lines) {
    }

    public record CorrectionLine(UUID receiptLineId, long previousQuantity, long quantity, long delta) {
    }

    public record ProductStock(UUID productId, List<VariantStock> variants) {
    }

    public record VariantStock(UUID variantId, String sku, List<StockOptionValue> optionValues, long quantity) {
    }

    public record StockOptionValue(UUID optionId, String optionName, UUID valueId, String label) {
    }
}
