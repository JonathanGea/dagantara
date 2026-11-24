package com.dagantara.backend.modules.storesettings.controller;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext.Scope;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_SHOP;
import static com.dagantara.backend.common.api.ApiRoutes.STORE_SETTINGS_PAGE;

@RestController
public class StoreSettingsPageController {
    private final TenantRequestContext contexts;
    private final String template;

    public StoreSettingsPageController(TenantRequestContext contexts) throws IOException {
        this.contexts = contexts;
        this.template = new ClassPathResource("admin/store-settings.html").getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping(value = {STORE_SETTINGS_PAGE, STOREFRONT_SHOP + STORE_SETTINGS_PAGE}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(HttpServletRequest request) {
        contexts.resolve(request).map(Scope::tenantId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found"));
        String nonce = UUID.randomUUID().toString();
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'none'; connect-src 'self'; "
                        + "script-src 'nonce-" + nonce + "'; style-src 'nonce-" + nonce + "'; "
                        + "base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
                .body(template.replace("{{nonce}}", nonce));
    }
}
