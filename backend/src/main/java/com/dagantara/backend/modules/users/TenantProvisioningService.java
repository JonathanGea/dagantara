package com.dagantara.backend.modules.users;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TenantProvisioningService {
    private final UserAccess access;
    private final UserManagementRepository management;
    private final UserService users;

    TenantProvisioningService(UserAccess access, UserManagementRepository management, UserService users) {
        this.access = access;
        this.management = management;
        this.users = users;
    }

    @Transactional
    public TenantController.TenantResponse provision(Jwt jwt, TenantController.CreateTenantRequest request) {
        UserAccess.Actor creator = access.require(jwt, UserAccess.TENANTS_PROVISION, true);
        if (creator.tenantId() != null) {
            throw UserAccess.forbidden();
        }
        UUID tenantId = management.createTenant(request.code(), request.name());
        UUID roleId = management.createTenantRole(tenantId, "TENANT_ADMIN", "Tenant Admin", true).id();
        management.replaceRolePermissions(roleId, UserAccess.TENANT_PERMISSIONS);
        TenantController.InitialAdmin admin = request.admin();
        UserAccount account = users.insertAccount(creator.account().getId(), tenantId, roleId,
                admin.username(), admin.email(), admin.fullName(), admin.password());
        return new TenantController.TenantResponse(tenantId, request.code(), CreateUserResponse.from(account));
    }
}
