package com.dagantara.backend.modules.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantProposalRequest(@NotBlank @Size(max = 253) String proposedDomain) {
}
