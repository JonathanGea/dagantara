package com.dagantara.backend.modules.users;

import com.dagantara.backend.common.exception.ApiException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RoleService {
    private final UserAccess access;
    private final UserManagementRepository management;

    RoleService(UserAccess access, UserManagementRepository management) {
        this.access = access;
        this.management = management;
    }

    @Transactional
    public UserManagementRepository.Role create(Jwt jwt, String code, String name) {
        UserAccess.Actor actor = access.require(jwt, UserAccess.ROLES_CREATE, true);
        UUID tenantId = actor.requireTenant();
        if ("TENANT_ADMIN".equals(code) || "SUPERADMIN".equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESERVED_ROLE_CODE", "The role code is reserved");
        }
        return management.createTenantRole(tenantId, code, name, false);
    }

    @Transactional
    public Set<String> replacePermissions(Jwt jwt, UUID roleId, Set<String> codes) {
        UserAccess.Actor actor = access.require(jwt, UserAccess.ROLES_PERMISSIONS_UPDATE, true);
        UserManagementRepository.Role role = management.findRole(roleId, actor.requireTenant(), "TENANT")
                .orElseThrow(UserAccess::notFound);
        if (role.system()) {
            throw new ApiException(HttpStatus.CONFLICT, "SYSTEM_ROLE_PROTECTED", "System role permissions are fixed");
        }
        actor.requireDelegation(management.rolePermissions(roleId));
        actor.requireDelegation(codes);
        management.replaceRolePermissions(roleId, codes);
        return codes;
    }
}
