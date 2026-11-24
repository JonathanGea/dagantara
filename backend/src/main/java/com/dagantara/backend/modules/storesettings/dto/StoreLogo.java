package com.dagantara.backend.modules.storesettings.dto;

import java.util.UUID;

import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_LOGO;

public record StoreLogo(UUID id, String contentType) {
    public String url(String shopSlug) {
        return (shopSlug == null ? "" : "/" + shopSlug) + STOREFRONT_LOGO + "/" + id;
    }
}
