package com.dagantara.backend.modules.inventory.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository.VariantRow;
import com.dagantara.backend.modules.catalog.service.CatalogAccess;
import com.dagantara.backend.modules.catalog.service.SkuCodes;
import com.dagantara.backend.modules.inventory.dto.CorrectInventoryRequest;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.Correction;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.CorrectionLine;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.OptionStock;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ProductStock;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.Receipt;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptDetail;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptDetailLine;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptLine;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptSummary;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.StockOptionValue;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ValueStock;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.VariantStock;
import com.dagantara.backend.modules.inventory.dto.ReceiveInventoryRequest;
import com.dagantara.backend.modules.inventory.repository.InventoryCorrectionRepository;
import com.dagantara.backend.modules.inventory.repository.InventoryReceiptRepository;
import com.dagantara.backend.modules.inventory.repository.InventoryStockRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.dagantara.backend.modules.tenants.TenantPermission.CORRECT_INVENTORY;
import static com.dagantara.backend.modules.tenants.TenantPermission.READ_INVENTORY;
import static com.dagantara.backend.modules.tenants.TenantPermission.RECEIVE_INVENTORY;

@Service
public class InventoryService {
    private final CatalogAccess access;
    private final CatalogRepository catalog;
    private final CatalogVariantRepository variants;
    private final InventoryReceiptRepository receipts;
    private final InventoryCorrectionRepository corrections;
    private final InventoryStockRepository stock;

    public InventoryService(CatalogAccess access, CatalogRepository catalog, CatalogVariantRepository variants,
                            InventoryReceiptRepository receipts, InventoryCorrectionRepository corrections,
                            InventoryStockRepository stock) {
        this.access = access;
        this.catalog = catalog;
        this.variants = variants;
        this.receipts = receipts;
        this.corrections = corrections;
        this.stock = stock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Receipt receive(AccountSession actor, String key, ReceiveInventoryRequest request) {
        UUID tenantId = access.require(actor, RECEIVE_INVENTORY);
        validateKey(key);
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "A receipt must contain at least one line");
        }
        List<IncomingLine> lines = request.lines().stream().map(InventoryService::validateLine).toList();
        byte[] hash = requestHash(lines);
        UUID receiptId = UUID.randomUUID();
        if (!receipts.claimKey(tenantId, actor.id(), key, hash, receiptId)) {
            var existing = receipts.getKey(tenantId, actor.id(), key);
            if (!MessageDigest.isEqual(hash, existing.requestHash())) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "Idempotency-Key has already been used with a different payload");
            }
            return receiptResponse(tenantId, requireReceipt(tenantId, existing.receiptId()));
        }

        List<VariantRow> locked = variants.lockBySkus(tenantId, lines.stream().map(IncomingLine::sku).toList());
        Map<String, VariantRow> bySku = locked.stream().collect(Collectors.toMap(VariantRow::sku, Function.identity()));
        Map<UUID, Long> quantities = new HashMap<>();
        for (IncomingLine line : lines) {
            VariantRow variant = bySku.get(line.sku());
            if (variant == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SKU", "An SKU was not found in this tenant");
            }
            quantities.merge(variant.id(), line.quantity(), InventoryService::addQuantities);
        }

        UUID warehouseId = stock.ensureDefaultWarehouse(tenantId);
        ReceiptSummary receipt = receipts.insertReceipt(tenantId, warehouseId, actor.id(), receiptId);
        for (VariantRow variant : locked) {
            long quantity = quantities.get(variant.id());
            receipts.insertLine(tenantId, receiptId, new ReceiptLine(UUID.randomUUID(), variant.id(),
                    variant.sku(), quantity));
            if (!stock.addReceiptQuantity(tenantId, warehouseId, variant.id(), quantity)) {
                throw quantityOutOfRange();
            }
        }
        variants.markStockRecorded(tenantId, quantities.keySet());
        return receiptResponse(tenantId, receipt);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Correction correct(AccountSession actor, UUID receiptId, String key, CorrectInventoryRequest request) {
        UUID tenantId = access.require(actor, CORRECT_INVENTORY);
        ReceiptSummary receipt = requireReceipt(tenantId, receiptId);
        validateKey(key);
        String reason = request.reason() == null ? null : request.reason().textValue();
        if (reason == null || reason.isBlank() || reason.indexOf('\0') >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A correction requires a reason");
        }
        Map<UUID, Long> requested = correctionQuantities(request);
        byte[] hash = correctionHash(receiptId, reason, requested);
        UUID correctionId = UUID.randomUUID();
        if (!corrections.claimKey(tenantId, actor.id(), key, hash, correctionId)) {
            var existing = corrections.getKey(tenantId, actor.id(), key);
            if (!MessageDigest.isEqual(hash, existing.requestHash())) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "Idempotency-Key has already been used with a different payload or receipt");
            }
            return corrections.getCorrection(tenantId, existing.correctionId());
        }

        // The receipt lock serializes calculation of effective quantities before any balance is updated.
        if (!receipts.lockReceipt(tenantId, receiptId)) {
            throw notFound("Receipt not found");
        }
        Map<UUID, Long> effective = corrections.effectiveQuantities(tenantId, receiptId, requested.keySet());
        if (effective.size() != requested.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECEIPT_LINE",
                    "Each line must belong to the original receipt");
        }
        corrections.insertCorrection(tenantId, receipt.warehouseId(), receiptId, actor.id(), correctionId, reason);
        // The receipt lines use the same PostgreSQL variant UUID order as receipt writers.
        for (ReceiptLine line : receipts.listLines(tenantId, receiptId)) {
            if (!requested.containsKey(line.id())) {
                continue;
            }
            long previous = effective.get(line.id());
            long quantity = requested.get(line.id());
            long delta = quantity - previous;
            if (!stock.applyCorrection(tenantId, receipt.warehouseId(), line.variantId(), delta)) {
                throw new ApiException(HttpStatus.CONFLICT, "STOCK_BALANCE_CONFLICT",
                        "Correction would cause an invalid stock balance");
            }
            corrections.insertLine(tenantId, receiptId, correctionId,
                    new CorrectionLine(line.id(), previous, quantity, delta));
        }
        return corrections.getCorrection(tenantId, correctionId);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<ReceiptSummary> listReceipts(AccountSession actor, Pageable page) {
        UUID tenantId = access.require(actor, READ_INVENTORY);
        return receipts.listReceipts(tenantId, PageableRules.enforce(page, Set.of()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReceiptDetail getReceipt(AccountSession actor, UUID receiptId) {
        UUID tenantId = access.require(actor, READ_INVENTORY);
        ReceiptSummary receipt = requireReceipt(tenantId, receiptId);
        List<Correction> history = corrections.listCorrections(tenantId, receiptId);
        Map<UUID, Long> effective = new HashMap<>();
        for (Correction correction : history) {
            for (CorrectionLine line : correction.lines()) {
                effective.put(line.receiptLineId(), line.quantity());
            }
        }
        List<ReceiptDetailLine> lines = receipts.listLines(tenantId, receiptId).stream()
                .map(line -> new ReceiptDetailLine(line.id(), line.variantId(), line.sku(),
                        line.quantity(), effective.getOrDefault(line.id(), line.quantity()))).toList();
        return new ReceiptDetail(receipt.id(), receipt.warehouseId(), receipt.receivedBy(),
                receipt.createdAt(), lines, history);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProductStock getProductStock(AccountSession actor, UUID productId) {
        UUID tenantId = access.require(actor, READ_INVENTORY);
        catalog.findProduct(tenantId, productId).orElseThrow(() -> notFound("Product not found"));
        List<VariantRow> rows = variants.listForProduct(tenantId, productId);
        Map<UUID, Long> quantities = stock.quantities(tenantId, rows.stream().map(VariantRow::id).toList());
        Map<UUID, StockOptionValue> values = new HashMap<>();
        var options = catalog.listOptions(tenantId, productId);
        for (var option : options) {
            for (var value : option.values()) {
                values.put(value.id(), new StockOptionValue(option.id(), option.name(), value.id(), value.label()));
            }
        }
        List<VariantStock> balances = rows.stream().map(variant -> new VariantStock(variant.id(), variant.sku(),
                variant.optionValueIds().stream().map(values::get).toList(), quantities.get(variant.id()))).toList();
        BigInteger total = BigInteger.ZERO;
        Map<UUID, BigInteger> valueTotals = new HashMap<>();
        for (VariantStock variant : balances) {
            BigInteger quantity = BigInteger.valueOf(variant.quantity());
            total = total.add(quantity);
            for (StockOptionValue value : variant.optionValues()) {
                valueTotals.merge(value.valueId(), quantity, BigInteger::add);
            }
        }
        List<OptionStock> optionTotals = options.stream().map(option -> new OptionStock(option.id(), option.name(),
                option.values().stream().filter(value -> valueTotals.containsKey(value.id()))
                        .map(value -> new ValueStock(value.id(), value.label(), valueTotals.get(value.id())))
                        .toList())).toList();
        return new ProductStock(productId, balances, total, optionTotals);
    }

    private ReceiptSummary requireReceipt(UUID tenantId, UUID receiptId) {
        return receipts.findReceipt(tenantId, receiptId).orElseThrow(() -> notFound("Receipt not found"));
    }

    private Receipt receiptResponse(UUID tenantId, ReceiptSummary receipt) {
        return new Receipt(receipt.id(), receipt.warehouseId(), receipt.receivedBy(), receipt.createdAt(),
                receipts.listLines(tenantId, receipt.id()));
    }

    private static IncomingLine validateLine(ReceiveInventoryRequest.Line line) {
        String inputSku = line.sku().textValue();
        String sku = SkuCodes.normalizeSku(inputSku);
        var quantity = line.quantity();
        if (!quantity.isIntegralNumber() || !quantity.canConvertToLong() || quantity.longValue() <= 0) {
            throw quantityOutOfRange();
        }
        return new IncomingLine(inputSku, sku, quantity.longValue());
    }

    private static long addQuantities(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw quantityOutOfRange();
        }
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key is required and must be at most 255 characters");
        }
    }

    private static Map<UUID, Long> correctionQuantities(CorrectInventoryRequest request) {
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "A correction must contain at least one line");
        }
        Map<UUID, Long> quantities = new LinkedHashMap<>();
        for (var line : request.lines()) {
            if (line == null || line.receiptLineId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECEIPT_LINE", "A receipt line ID is required");
            }
            var quantity = line.quantity();
            if (quantity == null || !quantity.isIntegralNumber() || !quantity.canConvertToLong()
                    || quantity.longValue() < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY",
                        "Corrected quantities must be nonnegative whole units");
            }
            if (quantities.putIfAbsent(line.receiptLineId(), quantity.longValue()) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECEIPT_LINE",
                        "A receipt line may appear only once in a correction");
            }
        }
        return quantities;
    }

    private static byte[] correctionHash(UUID receiptId, String reason, Map<UUID, Long> quantities) {
        StringBuilder payload = new StringBuilder().append(receiptId).append(':')
                .append(reason.length()).append(':').append(reason).append('\n');
        quantities.forEach((id, quantity) -> payload.append(id).append(':').append(quantity).append('\n'));
        return digest(payload);
    }

    private static byte[] requestHash(List<IncomingLine> lines) {
        // Order and submitted SKU spelling are part of the payload. Length prefixes keep separators unambiguous.
        StringBuilder payload = new StringBuilder();
        for (IncomingLine line : lines) {
            payload.append(line.inputSku().length()).append(':').append(line.inputSku())
                    .append(':').append(line.quantity()).append('\n');
        }
        return digest(payload);
    }

    private static byte[] digest(StringBuilder payload) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload.toString().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ApiException quantityOutOfRange() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY",
                "Receipt quantities must be positive whole units and fit in the stock balance");
    }

    private static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private record IncomingLine(String inputSku, String sku, long quantity) {
    }
}
