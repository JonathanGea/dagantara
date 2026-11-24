package com.dagantara.backend.modules.catalog.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CategoriesRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CategoryRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ChangeSkuRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateProductRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateVariantRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.MediaOptionValuesRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.MediaRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionValueRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ProductDetailsRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.PublicationRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Category;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Media;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.OptionValue;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductCategories;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductDetail;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductStatus;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductSummary;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Publication;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Variant;
import com.dagantara.backend.modules.catalog.service.CatalogMediaService;
import com.dagantara.backend.modules.catalog.service.CatalogService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping({ApiRoutes.V1 + "/catalog", "/{shopSlug:[a-z0-9-]+}" + ApiRoutes.V1 + "/catalog"})
public class CatalogController {
    private final CatalogService catalog;
    private final CatalogMediaService media;

    public CatalogController(CatalogService catalog, CatalogMediaService media) {
        this.catalog = catalog;
        this.media = media;
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @Valid @RequestBody CategoryRequest request) {
        return created("Category created", catalog.createCategory(actor, request.name()));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> listCategories(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Categories", catalog.listCategories(actor, page));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDetail>> createProduct(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @Valid @RequestBody CreateProductRequest request) {
        return created("Product draft created", catalog.createProduct(actor, request));
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductSummary>> listProducts(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Products", catalog.listProducts(actor, page));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDetail> getProduct(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId) {
        return ApiResponse.success("Product detail", catalog.getProduct(actor, productId));
    }

    @PutMapping("/products/{productId}/details")
    public ApiResponse<ProductDetail> updateDetails(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @Valid @RequestBody ProductDetailsRequest request) {
        return ApiResponse.success("Product details updated", catalog.updateDetails(actor, productId, request));
    }

    @PutMapping("/products/{productId}/publication")
    public ApiResponse<Publication> updatePublication(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @Valid @RequestBody PublicationRequest request) {
        return ApiResponse.success("Product publication updated",
                catalog.updatePublication(actor, productId, ProductStatus.valueOf(request.status())));
    }

    @PutMapping("/products/{productId}/categories")
    public ApiResponse<ProductCategories> replaceCategories(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @Valid @RequestBody CategoriesRequest request) {
        return ApiResponse.success("Product categories replaced", catalog.replaceCategories(actor, productId, request));
    }

    @PostMapping("/products/{productId}/options/{optionId}/values")
    public ResponseEntity<ApiResponse<OptionValue>> addOptionValue(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @PathVariable UUID optionId, @Valid @RequestBody OptionValueRequest request) {
        return created("Option value added", catalog.addOptionValue(actor, productId, optionId, request));
    }

    @PutMapping("/products/{productId}/options/{optionId}/values/{valueId}/details")
    public ApiResponse<OptionValue> updateOptionValue(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @PathVariable UUID optionId, @PathVariable UUID valueId, @Valid @RequestBody OptionValueRequest request) {
        return ApiResponse.success("Option value updated",
                catalog.updateOptionValue(actor, productId, optionId, valueId, request));
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiResponse<Variant>> createVariant(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @Valid @RequestBody CreateVariantRequest request) {
        return created("Variant created", catalog.createVariant(actor, productId, request));
    }

    @PutMapping("/products/{productId}/variants/{variantId}/sku")
    public ApiResponse<Variant> changeSku(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @PathVariable UUID variantId, @Valid @RequestBody ChangeSkuRequest request) {
        return ApiResponse.success("SKU updated", catalog.changeSku(actor, productId, variantId, request));
    }

    @PostMapping("/products/{productId}/media")
    public ResponseEntity<ApiResponse<Media>> addMedia(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @Valid @RequestBody MediaRequest request) {
        return created("Product media added", media.addMedia(actor, productId, request.url()));
    }

    @PutMapping("/products/{productId}/media/{mediaId}/option-values")
    public ApiResponse<Media> replaceMediaOptionValues(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @PathVariable UUID productId,
            @PathVariable UUID mediaId, @Valid @RequestBody MediaOptionValuesRequest request) {
        return ApiResponse.success("Media option values replaced",
                media.replaceOptionValues(actor, productId, mediaId, request.optionValueIds()));
    }

    private static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
    }
}
