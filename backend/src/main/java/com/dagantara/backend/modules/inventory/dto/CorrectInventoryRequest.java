package com.dagantara.backend.modules.inventory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CorrectInventoryRequest(@NotNull JsonNode reason, @NotEmpty List<@Valid @NotNull Line> lines) {
    public record Line(@NotNull UUID receiptLineId, @NotNull JsonNode quantity) {
    }
}
