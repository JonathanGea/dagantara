package com.dagantara.backend.modules.inventory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReceiveInventoryRequest(@NotEmpty List<@NotNull @Valid Line> lines) {
    public record Line(@NotNull JsonNode sku, @NotNull JsonNode quantity) {
    }
}
