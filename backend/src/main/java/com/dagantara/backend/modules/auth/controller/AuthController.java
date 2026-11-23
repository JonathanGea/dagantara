package com.dagantara.backend.modules.auth.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.common.security.AccountSessionFilter;
import com.dagantara.backend.modules.auth.dto.ChangeTemporaryPasswordRequest;
import com.dagantara.backend.modules.auth.dto.LoginRequest;
import com.dagantara.backend.modules.auth.dto.LoginResponse;
import com.dagantara.backend.modules.auth.service.AuthService;
import com.dagantara.backend.modules.tenants.service.TenantRequestContext;
import com.dagantara.backend.modules.users.dto.AccountSession;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ApiRoutes.V1 + "/auth", "/{shopSlug:[a-z0-9-]+}" + ApiRoutes.V1 + "/auth"})
public class AuthController {
    private final AuthService auth;
    private final TenantRequestContext contexts;

    public AuthController(AuthService auth, TenantRequestContext contexts) {
        this.auth = auth;
        this.contexts = contexts;
    }

    @Operation(summary = "Login akun platform atau tenant melalui URL yang sesuai")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.success("Login successful", auth.login(request, contexts.resolve(http)));
    }

    @PostMapping(path = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changePassword(
            @RequestAttribute(AccountSessionFilter.ACCOUNT_ATTRIBUTE) AccountSession session,
            @Valid @RequestBody ChangeTemporaryPasswordRequest request
    ) {
        auth.changeTemporaryPassword(session, request);
        return ResponseEntity.noContent().build();
    }
}
