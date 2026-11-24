package com.dagantara.backend.modules.storesettings.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.service.CatalogAccess;
import com.dagantara.backend.modules.storesettings.dto.StoreIdentity;
import com.dagantara.backend.modules.storesettings.dto.StoreLogo;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsRequest;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsResponse;
import com.dagantara.backend.modules.storesettings.repository.StoreSettingsRepository;
import com.dagantara.backend.modules.storesettings.repository.StoreSettingsRepository.StoredSettings;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.servlet.http.Part;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.dagantara.backend.modules.tenants.TenantPermission.MANAGE_STORE_SETTINGS;

@Service
public class StoreSettingsService {
    private final CatalogAccess access;
    private final StoreSettingsRepository settings;
    private final StoreLogoStorage storage;

    public StoreSettingsService(CatalogAccess access, StoreSettingsRepository settings, StoreLogoStorage storage) {
        this.access = access;
        this.settings = settings;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public StoreSettingsResponse read(AccountSession actor, String shopSlug) {
        return response(settings.find(access.require(actor, MANAGE_STORE_SETTINGS)), shopSlug);
    }

    @Transactional
    public StoreSettingsResponse save(AccountSession actor, String shopSlug, StoreSettingsRequest request,
                                      List<Part> logos) {
        UUID tenantId = access.require(actor, MANAGE_STORE_SETTINGS);
        boolean replace = "REPLACE".equals(request.logoAction());
        if (replace && (logos.size() != 1 || logos.getFirst().getSubmittedFileName() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "REPLACE requires exactly one logo file");
        }
        if (!replace && !logos.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Only REPLACE accepts a logo file");
        }

        settings.lockTenant(tenantId);
        StoredSettings previous = settings.find(tenantId);
        StoreLogo active = switch (request.logoAction()) {
            case "KEEP" -> previous.logo();
            case "REMOVE" -> null;
            case "REPLACE" -> storage.write(logos.getFirst());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid logo action");
        };
        if (!"KEEP".equals(request.logoAction())) {
            cleanUpOnCompletion(previous.logo(), active);
        }
        settings.save(tenantId, request, active);
        return response(settings.find(tenantId), shopSlug);
    }

    @Transactional(readOnly = true)
    public StoreIdentity publicIdentity(UUID tenantId, String shopSlug) {
        StoredSettings stored = settings.find(tenantId);
        String name = stored.displayName() == null ? stored.tenantName() : stored.displayName();
        return new StoreIdentity(name, stored.publicWhatsapp(), stored.publicEmail(), url(stored.logo(), shopSlug));
    }

    public Optional<PublicLogo> publicLogo(UUID tenantId, UUID logoId) {
        return settings.findActiveLogo(tenantId, logoId)
                .flatMap(logo -> storage.read(logo).map(bytes -> new PublicLogo(logo.contentType(), bytes)));
    }

    private void cleanUpOnCompletion(StoreLogo previous, StoreLogo active) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // A superseded ID cannot become active again. Retain both files if the commit outcome is unknown.
                if (status == STATUS_COMMITTED && previous != null) {
                    storage.discard(previous);
                } else if (status == STATUS_ROLLED_BACK && active != null) {
                    storage.discard(active);
                }
            }
        });
    }

    private static StoreSettingsResponse response(StoredSettings stored, String shopSlug) {
        return new StoreSettingsResponse(stored.tenantName(), stored.displayName(), stored.publicWhatsapp(),
                stored.publicEmail(), url(stored.logo(), shopSlug));
    }

    private static String url(StoreLogo logo, String shopSlug) {
        return logo == null ? null : logo.url(shopSlug);
    }

    public record PublicLogo(String contentType, byte[] bytes) {
    }
}
