package com.dagantara.backend.modules.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 128) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 255) String fullName,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(max = 72) String password,
        @NotNull UUID roleId
) {
    @Override
    public String toString() {
        return "CreateUserRequest";
    }
}
