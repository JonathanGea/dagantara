package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionTenantRequest(
        @NotBlank @Size(max = 255) String storeName,
        @NotBlank @Size(max = 32) String phoneNumber,
        @NotBlank @Size(max = 128) String ownerUsername,
        @NotBlank @Size(max = 2048) String loginUrl,
        @AssertTrue boolean loginUrlReady
) {
}
