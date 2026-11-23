package com.dagantara.backend.modules.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CatalogRequests {
    private CatalogRequests() {
    }

    public record CategoryRequest(@NotBlank @Size(max = 255) String name) {
    }

    public record CategoriesRequest(@NotEmpty Set<@NotNull UUID> categoryIds,
                                    @NotNull UUID primaryCategoryId) {
    }

    public record ProductDetailsRequest(@NotBlank @Size(max = 255) String name, String description,
                                        @PositiveOrZero @DecimalMax("9223372036854775807") BigDecimal priceIdr,
                                        String productCode) {
    }

    public record CreateProductRequest(@NotBlank @Size(max = 255) String name, String description,
                                       @PositiveOrZero @DecimalMax("9223372036854775807") BigDecimal priceIdr,
                                       @NotEmpty Set<@NotNull UUID> categoryIds, @NotNull UUID primaryCategoryId,
                                       String productCode, List<@NotNull @Valid OptionRequest> options,
                                       SkuMode skuMode, String sku) {
        public ProductDetailsRequest details() {
            return new ProductDetailsRequest(name, description, priceIdr, productCode);
        }

        public CategoriesRequest categories() {
            return new CategoriesRequest(categoryIds, primaryCategoryId);
        }
    }

    public record OptionRequest(@NotBlank @Size(max = 255) String name,
                                @NotNull List<@NotNull @Valid OptionValueRequest> values) {
    }

    public record OptionValueRequest(@NotBlank @Size(max = 255) String label, String code) {
    }

    public record CreateVariantRequest(@NotNull List<@NotNull UUID> optionValueIds,
                                       @NotNull SkuMode skuMode, String sku) {
    }

    public record ChangeSkuRequest(@NotNull SkuMode skuMode, String sku) {
    }

    public enum SkuMode {
        AUTO, MANUAL
    }
}
