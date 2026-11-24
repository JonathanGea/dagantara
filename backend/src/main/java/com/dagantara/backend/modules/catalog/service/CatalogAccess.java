package com.dagantara.backend.modules.catalog.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.tenants.TenantPermission;
import com.dagantara.backend.modules.tenants.repository.TenantAccountRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CatalogAccess {
    private final TenantAccountRepository accounts;

    public CatalogAccess(TenantAccountRepository accounts) {
        this.accounts = accounts;
    }

    public UUID require(AccountSession actor, TenantPermission permission) {
        if (actor.tenantId() == null) {
            throw forbidden();
        }
        var manager = accounts.findManager(actor).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Session is no longer valid"));
        if (!manager.permissions().contains(permission.code())) {
            throw forbidden();
        }
        return actor.tenantId();
    }

    private static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient tenant permissions");
    }
}
