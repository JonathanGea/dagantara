package com.dagantara.backend.modules.helpcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoveryLookupRequest(@NotBlank @Size(max = 2048) String loginUrl,
                                    @NotBlank @Size(max = 128) String username) {
    @Override
    public String toString() {
        return "RecoveryLookupRequest";
    }
}
