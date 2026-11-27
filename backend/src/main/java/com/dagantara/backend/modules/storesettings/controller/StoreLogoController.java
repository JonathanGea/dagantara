package com.dagantara.backend.modules.storesettings.controller;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.storesettings.service.StoreSettingsService;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_LOGO;
import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_SHOP;

@RestController
public class StoreLogoController {
    private final TenantRequestContext contexts;
    private final StoreSettingsService settings;

    public StoreLogoController(TenantRequestContext contexts, StoreSettingsService settings) {
        this.contexts = contexts;
        this.settings = settings;
    }

    @GetMapping({STOREFRONT_LOGO + "/{logoId}", STOREFRONT_SHOP + STOREFRONT_LOGO + "/{logoId}"})
    public ResponseEntity<byte[]> logo(HttpServletRequest request, @PathVariable String logoId) {
        UUID tenant = contexts.resolveActiveTenant(request).orElseThrow(StoreLogoController::notFound);
        var logo = settings.publicLogo(tenant, parseId(logoId)).orElseThrow(StoreLogoController::notFound);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.noStore()).body(logo.bytes());
    }

    private static UUID parseId(String value) {
        try {
            UUID id = UUID.fromString(value);
            if (id.toString().equalsIgnoreCase(value)) {
                return id;
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed public IDs have the same response as missing logos.
        }
        throw notFound();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }
}
