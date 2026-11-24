package com.dagantara.backend.modules.tenants.service;

import com.dagantara.backend.common.api.PageableRules;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.tenants.dto.TenantResponse;
import com.dagantara.backend.modules.tenants.dto.TenantResponse.Status;
import com.dagantara.backend.modules.tenants.repository.TenantRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.repository.AccountRepository;
import com.dagantara.backend.modules.users.repository.RolePermissionRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantManagementService {
    private final TenantRepository tenants;
    private final AccountRepository accounts;
    private final RolePermissionRepository permissions;

    public TenantManagementService(TenantRepository tenants, AccountRepository accounts,
                                   RolePermissionRepository permissions) {
        this.tenants = tenants;
        this.accounts = accounts;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TenantResponse> list(AccountSession actor, Pageable page) {
        requirePermission(actor, "tenants.read");
        return tenants.list(PageableRules.enforce(page, Set.of()));
    }

    @Transactional
    public TenantResponse deactivate(AccountSession actor, UUID tenantId) {
        requirePermission(actor, "tenants.deactivate");
        return changeStatus(tenantId, Status.INACTIVE);
    }

    @Transactional
    public TenantResponse reactivate(AccountSession actor, UUID tenantId) {
        requirePermission(actor, "tenants.activate");
        return changeStatus(tenantId, Status.ACTIVE);
    }

    private TenantResponse changeStatus(UUID tenantId, Status status) {
        TenantResponse tenant = tenants.lockTenant(tenantId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found"));
        if (tenant.status() == status) {
            return tenant;
        }
        tenants.updateStatus(tenantId, status);
        if (status == Status.INACTIVE) {
            accounts.revokeTenantSessions(tenantId);
        }
        return new TenantResponse(tenant.id(), tenant.name(), tenant.storeAddress(), status);
    }

    private void requirePermission(AccountSession actor, String permission) {
        if (actor.tenantId() != null || !permissions.hasPlatformPermission(actor.id(), permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Tenant management permission is required");
        }
    }
}
