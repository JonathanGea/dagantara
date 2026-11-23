package com.dagantara.backend.modules.users.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.common.security.UserIdentity;
import com.dagantara.backend.modules.users.entity.UserAccount;
import com.dagantara.backend.modules.users.repository.UserAccountRepository;
import com.dagantara.backend.modules.users.repository.UserManagementRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
class UserAccess {
    static final String USERS_CREATE = "users.create";
    static final String USERS_READ = "users.read";
    static final String USERS_ROLE_ASSIGN = "users.role.assign";
    static final String ROLES_CREATE = "roles.create";
    static final String ROLES_PERMISSIONS_UPDATE = "roles.permissions.update";
    static final String TENANTS_PROVISION = "tenants.provision";
    static final Set<String> TENANT_PERMISSIONS = Set.of(
            USERS_CREATE, USERS_READ, USERS_ROLE_ASSIGN, ROLES_CREATE, ROLES_PERMISSIONS_UPDATE);

    private final UserAccountRepository users;
    private final UserManagementRepository management;

    UserAccess(UserAccountRepository users, UserManagementRepository management) {
        this.users = users;
        this.management = management;
    }

    Actor require(Jwt jwt, String permission, boolean write) {
        UUID id = UserIdentity.userId(jwt).orElseThrow(UserAccess::forbidden);
        if (write) {
            // Serialize role, permission and account writes that use the same tenant's grants.
            management.lockActorTenant(id);
        }
        Set<String> permissions = management.effectivePermissions(id);
        if (!permissions.contains(permission)) {
            throw forbidden();
        }
        return new Actor(users.findById(id).orElseThrow(UserAccess::forbidden), permissions);
    }

    static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission for this operation");
    }

    static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    record Actor(UserAccount account, Set<String> permissions) {
        UUID tenantId() {
            return account.getTenantId();
        }

        UUID requireTenant() {
            if (tenantId() == null) {
                throw forbidden();
            }
            return tenantId();
        }

        void requireDelegation(Set<String> codes) {
            if (!permissions.containsAll(codes)
                    || (tenantId() != null && !TENANT_PERMISSIONS.containsAll(codes))) {
                throw forbidden();
            }
        }
    }
}
