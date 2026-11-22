package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteTenantApplicationRequest(
        @NotBlank @Size(max = 128) String ownerUsername,
        @NotBlank @Size(max = 2048) String loginUrl,
        @AssertTrue boolean loginUrlReady
) {
}
