package com.dagantara.backend.modules.storesettings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.CodePointLength;

public record StoreSettingsRequest(
        @CodePointLength(max = 255) @Pattern(regexp = "[^\\x00]*") String displayName,
        @Pattern(regexp = "\\+[1-9][0-9]{1,14}") String publicWhatsapp,
        @Email String publicEmail,
        @NotNull @Pattern(regexp = "KEEP") String logoAction
) {
    public StoreSettingsRequest {
        displayName = optionalText(displayName);
        publicWhatsapp = optionalText(publicWhatsapp);
        publicEmail = optionalText(publicEmail);
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
