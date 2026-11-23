package com.dagantara.backend.modules.users.service;

import com.dagantara.backend.modules.users.dto.CreateTenantRequest;
import com.dagantara.backend.modules.users.dto.CreateUserResponse;
import com.dagantara.backend.modules.users.dto.TenantResponse;
import com.dagantara.backend.modules.users.entity.UserAccount;
import com.dagantara.backend.modules.users.repository.UserManagementRepository;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantProvisioningService {
    private final UserAccess access;
    private final UserManagementRepository management;
    private final UserService users;

    TenantProvisioningService(UserAccess access, UserManagementRepository management, UserService users) {
        this.access = access;
        this.management = management;
        this.users = users;
    }

    @Transactional
    public TenantResponse provision(Jwt jwt, CreateTenantRequest request) {
        UserAccess.Actor creator = access.require(jwt, UserAccess.TENANTS_PROVISION, true);
        if (creator.tenantId() != null) {
            throw UserAccess.forbidden();
        }
        UUID tenantId = management.createTenant(request.code(), request.name());
        UUID roleId = management.createTenantRole(tenantId, "TENANT_ADMIN", "Tenant Admin", true).id();
        management.replaceRolePermissions(roleId, UserAccess.TENANT_PERMISSIONS);
        CreateTenantRequest.InitialAdmin admin = request.admin();
        UserAccount account = users.insertAccount(creator.account().getId(), tenantId, roleId,
                admin.username(), admin.email(), admin.fullName(), admin.password());
        return new TenantResponse(tenantId, request.code(), CreateUserResponse.from(account));
    }
}
