package com.dagantara.backend.modules.users.controller;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.AssignRoleRequest;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.CreateRequest;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.Credentials;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.Status;
import com.dagantara.backend.modules.users.dto.PlatformUserDtos.User;
import com.dagantara.backend.modules.users.service.PlatformOperatorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/users")
public class PlatformUserController {
    private final PlatformOperatorService operators;

    public PlatformUserController(PlatformOperatorService operators) {
        this.operators = operators;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Credentials>> create(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, @Valid @RequestBody CreateRequest request) {
        Credentials credentials = operators.createUser(actor, request.username(), request.roleId());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Operator created", credentials));
    }

    @GetMapping
    public ApiResponse<List<User>> list(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor, Pageable page) {
        return ApiResponse.paged("Operators found", operators.listUsers(actor, page));
    }

    @GetMapping("/{userId}")
    public ApiResponse<User> detail(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                    @PathVariable UUID userId) {
        return ApiResponse.success("Operator found", operators.getUser(actor, userId));
    }

    @PutMapping("/{userId}/role")
    public ApiResponse<User> assignRole(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                        @PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest request) {
        return ApiResponse.success("Role assigned", operators.assignRole(actor, userId, request.roleId()));
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Credentials>> resetPassword(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                                                   @PathVariable UUID userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Operator password reset", operators.resetPassword(actor, userId)));
    }

    @PostMapping("/{userId}/deactivate")
    public ApiResponse<User> deactivate(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                        @PathVariable UUID userId) {
        return ApiResponse.success("Operator deactivated", operators.changeStatus(actor, userId, Status.INACTIVE));
    }

    @PostMapping("/{userId}/reactivate")
    public ApiResponse<User> reactivate(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                        @PathVariable UUID userId) {
        return ApiResponse.success("Operator reactivated", operators.changeStatus(actor, userId, Status.ACTIVE));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<User> archive(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
                                     @PathVariable UUID userId) {
        return ApiResponse.success("Operator archived", operators.changeStatus(actor, userId, Status.ARCHIVED));
    }
}
