package com.dagantara.backend.modules.storesettings.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.service.CatalogAccess;
import com.dagantara.backend.modules.storesettings.dto.StoreIdentity;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsRequest;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsResponse;
import com.dagantara.backend.modules.storesettings.repository.StoreSettingsRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.dagantara.backend.modules.tenants.TenantPermission.MANAGE_STORE_SETTINGS;

@Service
public class StoreSettingsService {
    private final CatalogAccess access;
    private final StoreSettingsRepository settings;

    public StoreSettingsService(CatalogAccess access, StoreSettingsRepository settings) {
        this.access = access;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public StoreSettingsResponse read(AccountSession actor) {
        return settings.find(access.require(actor, MANAGE_STORE_SETTINGS));
    }

    @Transactional
    public StoreSettingsResponse save(AccountSession actor, StoreSettingsRequest request, boolean hasLogo) {
        UUID tenantId = access.require(actor, MANAGE_STORE_SETTINGS);
        if (hasLogo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "KEEP does not accept a logo file");
        }
        settings.save(tenantId, request);
        return settings.find(tenantId);
    }

    @Transactional(readOnly = true)
    public StoreIdentity publicIdentity(UUID tenantId) {
        StoreSettingsResponse stored = settings.find(tenantId);
        String name = stored.displayName() == null ? stored.tenantName() : stored.displayName();
        return new StoreIdentity(name, stored.publicWhatsapp(), stored.publicEmail());
    }
}
