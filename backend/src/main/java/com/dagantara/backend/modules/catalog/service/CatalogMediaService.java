package com.dagantara.backend.modules.catalog.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Media;
import com.dagantara.backend.modules.catalog.repository.CatalogMediaRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.dagantara.backend.modules.tenants.TenantPermission.MANAGE_CATALOG;

@Service
public class CatalogMediaService {
    private final CatalogAccess access;
    private final CatalogMediaRepository media;

    public CatalogMediaService(CatalogAccess access, CatalogMediaRepository media) {
        this.access = access;
        this.media = media;
    }

    @Transactional
    public Media addMedia(AccountSession actor, UUID productId, String url) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        String validatedUrl = validateUrl(url);
        return media.insert(tenantId, productId, validatedUrl)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Product not found"));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Media replaceOptionValues(AccountSession actor, UUID productId, UUID mediaId, Set<UUID> valueIds) {
        UUID tenantId = access.require(actor, MANAGE_CATALOG);
        // Serialize replacements for this media so a waiting writer sees the committed links.
        String url = media.lockUrl(tenantId, productId, mediaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Media not found"));
        var savedIds = media.replaceOptionValues(tenantId, productId, mediaId, valueIds);
        if (savedIds.size() != valueIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPTION_VALUES",
                    "Choose option values from the product that owns this media");
        }
        return new Media(mediaId, url, savedIds);
    }

    private static String validateUrl(String url) {
        String value = url.strip();
        try {
            URI uri = new URI(value);
            if (uri.getHost() != null && uri.getPort() <= 65535
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return value;
            }
        } catch (URISyntaxException invalid) {
            throw invalidUrl();
        }
        throw invalidUrl();
    }

    private static ApiException invalidUrl() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA_URL",
                "Media URL must be an absolute HTTP or HTTPS URL");
    }
}
