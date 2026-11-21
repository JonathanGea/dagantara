package com.dagantara.backend.modules.users;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/tenants")
public class TenantController {
    private final TenantProvisioningService tenants;

    TenantController(TenantProvisioningService tenants) {
        this.tenants = tenants;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantResponse> provision(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.success("Tenant provisioned", tenants.provision(jwt, request));
    }

    public record CreateTenantRequest(@NotBlank @Size(max = 128) String code,
                                      @NotBlank @Size(max = 255) String name,
                                      @NotNull @Valid InitialAdmin admin) {
    }

    public record InitialAdmin(
            @NotBlank @Size(max = 128) String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 255) String fullName,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            @NotBlank @Size(max = 72) String password
    ) {
        @Override
        public String toString() {
            return "InitialAdmin";
        }
    }

    public record TenantResponse(UUID id, String code, CreateUserResponse admin) {
    }
}
