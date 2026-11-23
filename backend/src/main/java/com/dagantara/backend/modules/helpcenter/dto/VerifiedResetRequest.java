package com.dagantara.backend.modules.helpcenter.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

public record VerifiedResetRequest(@NotNull VerificationMethod verificationMethod) {
    public enum VerificationMethod {
        MATCHED_INCOMING_NUMBER, CALLBACK_TO_REGISTERED_NUMBER, KNOWN_REQUESTER;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static VerificationMethod parse(String value) {
            return valueOf(value);
        }
    }
}
