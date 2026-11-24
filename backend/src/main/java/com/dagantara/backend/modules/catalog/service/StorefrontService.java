package com.dagantara.backend.modules.catalog.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.Option;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.OptionValue;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.ProductCard;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.ProductDetail;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.Variant;
import com.dagantara.backend.modules.catalog.repository.CatalogMediaRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository.VariantRow;
import com.dagantara.backend.modules.catalog.repository.StorefrontRepository;
import com.dagantara.backend.modules.inventory.service.InventoryAvailabilityService;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StorefrontService {
    private final StorefrontRepository storefront;
    private final CatalogRepository catalog;
    private final CatalogMediaRepository media;
    private final CatalogVariantRepository variants;
    private final InventoryAvailabilityService inventory;

    public StorefrontService(StorefrontRepository storefront, CatalogRepository catalog, CatalogMediaRepository media,
                             CatalogVariantRepository variants, InventoryAvailabilityService inventory) {
        this.storefront = storefront;
        this.catalog = catalog;
        this.media = media;
        this.variants = variants;
        this.inventory = inventory;
    }

    public Page<ProductCard> list(UUID tenantId, Pageable page) {
        return storefront.listPublished(tenantId, PageableRules.enforce(page, Set.of()));
    }

    public ProductDetail detail(UUID tenantId, UUID productId) {
        var product = catalog.findPublishedProduct(tenantId, productId).orElseThrow(StorefrontService::notFound);
        var options = catalog.listOptions(tenantId, productId).stream()
                .map(option -> new Option(option.id(), option.name(), option.values().stream()
                        .map(value -> new OptionValue(value.id(), value.label())).toList()))
                .toList();
        var registered = variants.listForProduct(tenantId, productId);
        var availability = inventory.availability(tenantId, registered.stream().map(VariantRow::id).toList());
        var combinations = registered.stream()
                .map(variant -> new Variant(variant.id(), variant.optionValueIds(), availability.get(variant.id())))
                .toList();
        return new ProductDetail(productId, product.name(), product.priceIdr(), product.description(),
                catalog.listProductCategories(tenantId, productId), product.primaryCategoryId(),
                media.listForProduct(tenantId, productId), options, combinations);
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }
}
