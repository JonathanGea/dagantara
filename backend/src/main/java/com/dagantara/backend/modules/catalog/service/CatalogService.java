package com.dagantara.backend.modules.catalog.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CategoriesRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ChangeSkuRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateProductRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateVariantRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionValueRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ProductDetailsRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.SkuMode;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Category;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.OptionValue;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductCategories;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductDetail;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductStatus;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductSummary;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Variant;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository.Product;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository.SelectedValue;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository.VariantRow;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.dagantara.backend.modules.tenants.TenantPermission.MANAGE_CATALOG;
import static com.dagantara.backend.modules.tenants.TenantPermission.READ_CATALOG;

@Service
public class CatalogService {
    private final CatalogAccess access;
    private final CatalogRepository catalog;
    private final CatalogVariantRepository variants;

    public CatalogService(CatalogAccess access, CatalogRepository catalog, CatalogVariantRepository variants) {
        this.access = access;
        this.catalog = catalog;
        this.variants = variants;
    }

    @Transactional
    public Category createCategory(AccountSession actor, String name) {
        return catalog.insertCategory(access.require(actor, MANAGE_CATALOG), name.strip());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<Category> listCategories(AccountSession actor, Pageable page) {
        return catalog.listCategories(access.require(actor, READ_CATALOG), PageableRules.enforce(page, Set.of()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<ProductSummary> listProducts(AccountSession actor, Pageable page) {
        return catalog.listProducts(access.require(actor, READ_CATALOG), PageableRules.enforce(page, Set.of()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProductDetail getProduct(AccountSession actor, UUID productId) {
        UUID tenantId = access.require(actor, READ_CATALOG);
        return detail(tenantId, catalog.findProduct(tenantId, productId)
                .orElseThrow(() -> notFound("Product")));
    }

    @Transactional
    public ProductDetail createProduct(AccountSession actor, CreateProductRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        validateCategories(tenantId, request.categories());
        UUID productId = UUID.randomUUID();
        Product product = productDetails(productId, ProductStatus.DRAFT, request.primaryCategoryId(), request.details());
        catalog.insertProduct(tenantId, product);
        catalog.replaceCategories(tenantId, productId, request.categoryIds(), request.primaryCategoryId());

        List<OptionRequest> options = request.options() == null ? List.of() : request.options();
        if (options.isEmpty()) {
            SkuMode mode = request.skuMode() == null ? SkuMode.AUTO : request.skuMode();
            variants.insert(tenantId, productId, mode, createSku(product, List.of(), mode, request.sku()), List.of());
        } else {
            if (request.skuMode() != null || request.sku() != null) {
                throw invalid("INVALID_VARIANT", "Register SKUs separately for products with options");
            }
            for (int position = 0; position < options.size(); position++) {
                var option = options.get(position);
                UUID optionId = catalog.insertOption(tenantId, productId, option.name().strip(), position);
                for (var value : option.values()) {
                    insertValue(tenantId, productId, optionId, value);
                }
            }
        }
        return detail(tenantId, product);
    }

    @Transactional
    public ProductDetail updateDetails(AccountSession actor, UUID productId, ProductDetailsRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        Product stored = lockProduct(tenantId, productId);
        Product updated = productDetails(productId, stored.status(), stored.primaryCategoryId(), request);
        if (updated.status() == ProductStatus.PUBLISHED
                && (updated.priceIdr() == null || variants.listForProduct(tenantId, productId).isEmpty())) {
            throw conflict("PRODUCT_NOT_READY", "A published product requires a price and at least one variant");
        }
        catalog.updateDetails(tenantId, productId, updated.name(), updated.description(),
                updated.priceIdr(), updated.productCode());
        return detail(tenantId, updated);
    }

    @Transactional
    public ProductCategories replaceCategories(AccountSession actor, UUID productId, CategoriesRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        lockProduct(tenantId, productId);
        validateCategories(tenantId, request);
        catalog.replaceCategories(tenantId, productId, request.categoryIds(), request.primaryCategoryId());
        return new ProductCategories(catalog.listCategoryIds(tenantId, productId), request.primaryCategoryId());
    }

    @Transactional
    public OptionValue addOptionValue(AccountSession actor, UUID productId, UUID optionId, OptionValueRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        lockProduct(tenantId, productId);
        if (!catalog.containsOption(tenantId, productId, optionId)) {
            throw notFound("Option");
        }
        return insertValue(tenantId, productId, optionId, request);
    }

    @Transactional
    public OptionValue updateOptionValue(AccountSession actor, UUID productId, UUID optionId, UUID valueId,
                                         OptionValueRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        lockProduct(tenantId, productId);
        String label = request.label().strip();
        String code = SkuCodes.normalizeShortCode(request.code());
        if (!catalog.updateOptionValue(tenantId, productId, optionId, valueId, label, code)) {
            throw notFound("Option value");
        }
        return new OptionValue(valueId, label, code);
    }

    @Transactional
    public Variant createVariant(AccountSession actor, UUID productId, CreateVariantRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        Product product = lockProduct(tenantId, productId);
        List<SelectedValue> values = catalog.findSelectedValues(tenantId, productId, request.optionValueIds());
        long optionCount = catalog.countOptions(tenantId, productId);
        if (values.size() != request.optionValueIds().size()
                || values.stream().map(SelectedValue::optionId).distinct().count() != optionCount
                || values.size() != optionCount) {
            throw invalid("INVALID_COMBINATION", "Select one value from each option of this product");
        }
        String sku = createSku(product, values, request.skuMode(), request.sku());
        return variants.insert(tenantId, productId, request.skuMode(), sku, values);
    }

    @Transactional
    public Variant changeSku(AccountSession actor, UUID productId, UUID variantId, ChangeSkuRequest request) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        Product product = lockProduct(tenantId, productId);
        VariantRow variant = variants.lockForProduct(tenantId, productId, variantId)
                .orElseThrow(() -> notFound("Variant"));
        if (variant.stockRecorded()) {
            throw skuLocked();
        }
        List<SelectedValue> values = catalog.findSelectedValues(tenantId, productId, variant.optionValueIds());
        String sku = createSku(product, values, request.skuMode(), request.sku());
        if (!variants.changeSku(tenantId, variantId, request.skuMode(), sku)) {
            throw skuLocked();
        }
        return new Variant(variantId, variant.optionValueIds(), sku, request.skuMode());
    }

    private Product lockProduct(UUID tenantId, UUID productId) {
        return catalog.lockProduct(tenantId, productId).orElseThrow(() -> notFound("Product"));
    }

    private ProductDetail detail(UUID tenantId, Product product) {
        UUID id = product.id();
        return new ProductDetail(id, product.name(), product.description(), product.priceIdr(), product.productCode(),
                product.status(), catalog.listCategoryIds(tenantId, id), product.primaryCategoryId(),
                catalog.listOptions(tenantId, id), variants.listForProduct(tenantId, id).stream()
                        .map(VariantRow::toResponse).toList());
    }

    private void validateCategories(UUID tenantId, CategoriesRequest request) {
        if (!request.categoryIds().contains(request.primaryCategoryId())
                || !catalog.containsCategories(tenantId, request.categoryIds())) {
            throw invalid("INVALID_CATEGORIES", "Choose categories from this catalogue and select one as primary");
        }
    }

    private OptionValue insertValue(UUID tenantId, UUID productId, UUID optionId, OptionValueRequest request) {
        return catalog.insertOptionValue(tenantId, productId, optionId, request.label().strip(),
                SkuCodes.normalizeShortCode(request.code()));
    }

    private static Product productDetails(UUID id, ProductStatus status, UUID primaryCategoryId,
                                          ProductDetailsRequest request) {
        Long price = null;
        if (request.priceIdr() != null) {
            try {
                price = request.priceIdr().longValueExact();
            } catch (ArithmeticException exception) {
                throw invalid("INVALID_PRICE", "Price must be an integer in rupiah");
            }
        }
        return new Product(id, request.name().strip(), request.description(), price,
                SkuCodes.normalizeShortCode(request.productCode()), status, primaryCategoryId);
    }

    private static String createSku(Product product, List<SelectedValue> values, SkuMode mode, String manualSku) {
        if (mode == SkuMode.MANUAL) {
            return SkuCodes.normalizeSku(manualSku);
        }
        StringJoiner codes = new StringJoiner("-");
        codes.add(requiredCode(product.productCode()));
        values.forEach(value -> codes.add(requiredCode(value.value().code())));
        return SkuCodes.normalizeSku(codes.toString());
    }

    private static String requiredCode(String code) {
        if (code == null) {
            throw invalid("MISSING_SHORT_CODE", "An automatic SKU requires short codes for the product and its values");
        }
        return code;
    }

    private static ApiException skuLocked() {
        return conflict("SKU_LOCKED", "SKU cannot be changed after stock has been recorded");
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private static ApiException invalid(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static ApiException notFound(String resource) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " not found");
    }
}
