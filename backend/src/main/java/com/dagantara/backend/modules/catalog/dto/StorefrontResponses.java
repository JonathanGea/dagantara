package com.dagantara.backend.modules.catalog.dto;

import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Category;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Media;
import com.dagantara.backend.modules.inventory.service.InventoryAvailabilityService.Availability;
import java.util.List;
import java.util.UUID;

public final class StorefrontResponses {
    private StorefrontResponses() {
    }

    public record ProductCard(UUID id, String name, long priceIdr, String imageUrl) {
    }

    public record ProductDetail(UUID id, String name, long priceIdr, String description, List<Category> categories,
                                UUID primaryCategoryId, List<Media> media, List<Option> options,
                                List<Variant> variants) {
    }

    public record Option(UUID id, String name, List<OptionValue> values) {
    }

    public record OptionValue(UUID id, String label) {
    }

    public record Variant(UUID id, List<UUID> optionValueIds, Availability availability) {
    }
}
