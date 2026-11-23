package com.dagantara.backend.modules.catalog.dto;

import com.dagantara.backend.modules.catalog.dto.CatalogRequests.SkuMode;
import java.util.List;
import java.util.UUID;

public final class CatalogResponses {
    private CatalogResponses() {
    }

    public record Category(UUID id, String name) {
    }

    public record ProductCategories(List<UUID> categoryIds, UUID primaryCategoryId) {
    }

    public record ProductSummary(UUID id, String name, Long priceIdr, ProductStatus status, UUID primaryCategoryId) {
    }

    public record ProductDetail(UUID id, String name, String description, Long priceIdr, String productCode,
                                ProductStatus status, List<UUID> categoryIds, UUID primaryCategoryId,
                                List<Option> options, List<Variant> variants, List<Media> media) {
    }

    public record Media(UUID id, String url, List<UUID> optionValueIds) {
    }

    public record Option(UUID id, String name, List<OptionValue> values) {
    }

    public record OptionValue(UUID id, String label, String code) {
    }

    public record Variant(UUID id, List<UUID> optionValueIds, String sku, SkuMode skuMode) {
    }

    public enum ProductStatus {
        DRAFT, PUBLISHED
    }
}
