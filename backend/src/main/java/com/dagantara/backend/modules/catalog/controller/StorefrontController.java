package com.dagantara.backend.modules.catalog.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.ProductCard;
import com.dagantara.backend.modules.catalog.dto.StorefrontResponses.ProductDetail;
import com.dagantara.backend.modules.catalog.service.StorefrontService;
import com.dagantara.backend.modules.storesettings.dto.StoreIdentity;
import com.dagantara.backend.modules.storesettings.service.StoreSettingsService;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext.Scope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_API_PRODUCTS;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_PAGE_PRODUCTS;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_PAGE_PRODUCT_ID;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_SHOP;

@RestController
public class StorefrontController {
    private final TenantRequestContext contexts;
    private final StorefrontService storefront;
    private final StoreSettingsService settings;
    private final ObjectWriter pageWriter;
    private final String template;

    public StorefrontController(TenantRequestContext contexts, StorefrontService storefront,
                                StoreSettingsService settings, ObjectMapper mapper) throws IOException {
        this.contexts = contexts;
        this.storefront = storefront;
        this.settings = settings;
        // Preserve integer rupiah prices beyond JavaScript's safe integer range.
        this.pageWriter = mapper.writer().with(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature());
        this.template = new ClassPathResource("storefront/products.html").getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping({STOREFRONT_API_PRODUCTS, STOREFRONT_SHOP + STOREFRONT_API_PRODUCTS})
    public ApiResponse<List<ProductCard>> list(HttpServletRequest request, Pageable page) {
        return ApiResponse.paged("Products", storefront.list(tenantId(request), page));
    }

    @GetMapping({STOREFRONT_API_PRODUCTS + "/{productId}", STOREFRONT_SHOP + STOREFRONT_API_PRODUCTS + "/{productId}"})
    public ApiResponse<ProductDetail> detail(HttpServletRequest request, @PathVariable String productId) {
        return ApiResponse.success("Product detail", storefront.detail(tenantId(request), parseId(productId)));
    }

    @GetMapping(value = {STOREFRONT_PAGE_PRODUCTS, STOREFRONT_SHOP + STOREFRONT_PAGE_PRODUCTS,
            STOREFRONT_PAGE_PRODUCTS + STOREFRONT_PAGE_PRODUCT_ID,
            STOREFRONT_SHOP + STOREFRONT_PAGE_PRODUCTS + STOREFRONT_PAGE_PRODUCT_ID}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(HttpServletRequest request, Pageable page,
                                       @PathVariable(required = false) String productId,
                                       @PathVariable(required = false) String shopSlug) throws JsonProcessingException {
        UUID tenant = tenantId(request);
        ApiResponse<?> catalog = productId == null
                ? ApiResponse.paged("Products", storefront.list(tenant, page))
                : ApiResponse.success("Product detail", storefront.detail(tenant, parseId(productId)));
        var data = new PageData(settings.publicIdentity(tenant, shopSlug), catalog);
        String nonce = UUID.randomUUID().toString();
        String json = pageWriter.writeValueAsString(data).replace("<", "\\u003c");
        String html = template.replace("{{nonce}}", nonce).replace("{{data}}", json);
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'none'; img-src http: https:; "
                        + "script-src 'nonce-" + nonce + "'; style-src 'nonce-" + nonce + "'; "
                        + "base-uri 'none'; frame-ancestors 'none'")
                .header("Referrer-Policy", "no-referrer")
                .body(html);
    }

    private record PageData(StoreIdentity store, ApiResponse<?> catalog) {
    }

    private UUID tenantId(HttpServletRequest request) {
        return contexts.resolve(request).map(Scope::tenantId).orElseThrow(StorefrontService::notFound);
    }

    private static UUID parseId(String value) {
        try {
            UUID id = UUID.fromString(value);
            if (id.toString().equalsIgnoreCase(value)) {
                return id;
            }
        } catch (IllegalArgumentException ignored) {
            // Treat malformed and missing public IDs alike.
        }
        throw StorefrontService.notFound();
    }
}
