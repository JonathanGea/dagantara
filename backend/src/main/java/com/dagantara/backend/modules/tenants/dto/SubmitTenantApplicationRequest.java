package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitTenantApplicationRequest(
        @NotBlank @Size(max = 255) String storeName,
        @NotBlank @Size(max = 32) String phoneNumber,
        @NotBlank @Size(max = 253) String proposedDomain
) {
}
