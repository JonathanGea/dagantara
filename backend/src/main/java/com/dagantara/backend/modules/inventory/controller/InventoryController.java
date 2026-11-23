package com.dagantara.backend.modules.inventory.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ProductStock;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.Receipt;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptDetail;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ReceiptSummary;
import com.dagantara.backend.modules.inventory.dto.ReceiveInventoryRequest;
import com.dagantara.backend.modules.inventory.service.InventoryService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping({ApiRoutes.V1 + "/inventory", "/{shopSlug:[a-z0-9-]+}" + ApiRoutes.V1 + "/inventory"})
public class InventoryController {
    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @PostMapping("/receipts")
    public ResponseEntity<ApiResponse<Receipt>> receive(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ReceiveInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Receipt recorded", inventory.receive(actor, key, request)));
    }

    @GetMapping("/receipts")
    public ApiResponse<List<ReceiptSummary>> listReceipts(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Receipts", inventory.listReceipts(actor, page));
    }

    @GetMapping("/receipts/{receiptId}")
    public ApiResponse<ReceiptDetail> getReceipt(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                                @PathVariable UUID receiptId) {
        return ApiResponse.success("Receipt detail", inventory.getReceipt(actor, receiptId));
    }

    @GetMapping("/products/{productId}/stock")
    public ApiResponse<ProductStock> getProductStock(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                                    @PathVariable UUID productId) {
        return ApiResponse.success("Product stock", inventory.getProductStock(actor, productId));
    }
}
