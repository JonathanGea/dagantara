package com.dagantara.backend.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 128) String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(max = 1024) String password,
        @Size(min = 1) String tenantCode
) {
    @Override
    public String toString() {
        return "LoginRequest";
    }
}
