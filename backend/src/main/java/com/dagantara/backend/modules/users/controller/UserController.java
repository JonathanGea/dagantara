package com.dagantara.backend.modules.users.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.users.dto.AssignRoleRequest;
import com.dagantara.backend.modules.users.dto.CreateUserRequest;
import com.dagantara.backend.modules.users.dto.CreateUserResponse;
import com.dagantara.backend.modules.users.dto.RoleAssignmentResponse;
import com.dagantara.backend.modules.users.dto.UserDetailsResponse;
import com.dagantara.backend.modules.users.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateUserResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", users.create(jwt, request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserDetailsResponse> details(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID userId) {
        return ApiResponse.success("User found", users.details(jwt, userId));
    }

    @PutMapping(path = "/{userId}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<RoleAssignmentResponse> assignRole(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable UUID userId,
                                                          @Valid @RequestBody AssignRoleRequest request) {
        users.assignRole(jwt, userId, request.roleId());
        return ApiResponse.success("Role assigned", new RoleAssignmentResponse(userId, request.roleId()));
    }
}
