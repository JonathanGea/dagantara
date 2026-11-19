package com.dagantara.backend.modules.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 128) String username,
        @NotBlank @Size(max = 1024) String password
) {
}
