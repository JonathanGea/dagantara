package com.dagantara.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeTemporaryPasswordRequest(@NotBlank @Size(max = 72) String newPassword) {
    @Override
    public String toString() {
        return "ChangeTemporaryPasswordRequest";
    }
}
