package com.dagantara.backend.modules.users.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.users.dto.CreateUserRequest;
import com.dagantara.backend.modules.users.dto.CreateUserResponse;
import com.dagantara.backend.modules.users.dto.UserDetailsResponse;
import com.dagantara.backend.modules.users.entity.UserAccount;
import com.dagantara.backend.modules.users.repository.UserAccountRepository;
import com.dagantara.backend.modules.users.repository.UserManagementRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserAccountRepository users;
    private final UserManagementRepository management;
    private final UserAccess access;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository users, UserManagementRepository management,
                       UserAccess access, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.management = management;
        this.access = access;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse create(Jwt jwt, CreateUserRequest request) {
        UserAccess.Actor creator = access.require(jwt, UserAccess.USERS_CREATE, true);
        requireAssignableRole(creator, request.roleId());
        return CreateUserResponse.from(insertAccount(creator.account().getId(), creator.tenantId(),
                request.roleId(), request.username(), request.email(), request.fullName(), request.password()));
    }

    @Transactional(readOnly = true)
    public UserDetailsResponse details(Jwt jwt, UUID userId) {
        UserAccess.Actor actor = access.require(jwt, UserAccess.USERS_READ, false);
        return UserDetailsResponse.from(findTenantUser(userId, actor.requireTenant()));
    }

    @Transactional
    public void assignRole(Jwt jwt, UUID userId, UUID roleId) {
        UserAccess.Actor actor = access.require(jwt, UserAccess.USERS_ROLE_ASSIGN, true);
        UserAccount target = findTenantUser(userId, actor.requireTenant());
        actor.requireDelegation(management.rolePermissions(target.getRoleId()));
        requireAssignableRole(actor, roleId);
        target.assignRole(roleId);
    }

    UserAccount insertAccount(UUID creatorId, UUID tenantId, UUID roleId, String username,
                              String email, String fullName, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must be at most 72 UTF-8 bytes");
        }
        return users.saveAndFlush(new UserAccount(creatorId, tenantId, roleId, username, email,
                fullName, passwordEncoder.encode(password)));
    }

    private UserAccount findTenantUser(UUID userId, UUID tenantId) {
        return users.findByIdAndTenantId(userId, tenantId).orElseThrow(UserAccess::notFound);
    }

    private void requireAssignableRole(UserAccess.Actor actor, UUID roleId) {
        management.findRole(roleId, actor.tenantId(), actor.account().getAccountType())
                .filter(role -> !"SUPERADMIN".equals(role.code()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE",
                        "The selected role is not valid for this account"));
        actor.requireDelegation(management.rolePermissions(roleId));
    }
}
