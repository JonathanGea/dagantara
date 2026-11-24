package com.dagantara.backend.common.api;

/** Stable, versioned HTTP route prefixes. */
public final class ApiRoutes {
    public static final String V1 = "/api/v1";
    public static final String STOREFRONT_API_PRODUCTS = V1 + "/storefront/products";
    public static final String STOREFRONT_PAGE_PRODUCTS = "/products";
    public static final String STOREFRONT_SHOP = "/{shopSlug:[a-z0-9][a-z0-9-]{0,62}}";
    // A provisioned shop can be named "products".
    public static final String STOREFRONT_PAGE_PRODUCT_ID = "/{productId:[0-9a-fA-F-]{36}}";

    private ApiRoutes() {
    }
}
