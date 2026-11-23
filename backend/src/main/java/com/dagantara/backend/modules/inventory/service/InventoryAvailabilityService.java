package com.dagantara.backend.modules.inventory.service;

import com.dagantara.backend.modules.inventory.repository.InventoryStockRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InventoryAvailabilityService {
    private final InventoryStockRepository stock;

    public InventoryAvailabilityService(InventoryStockRepository stock) {
        this.stock = stock;
    }

    /** The caller supplies the server-resolved tenant. Unknown and foreign variant IDs are omitted. */
    public Map<UUID, Availability> availability(UUID tenantId, Collection<UUID> variantIds) {
        Map<UUID, Availability> statuses = new HashMap<>();
        stock.quantities(tenantId, variantIds).forEach((id, quantity) ->
                statuses.put(id, quantity > 0 ? Availability.IN_STOCK : Availability.OUT_OF_STOCK));
        return Map.copyOf(statuses);
    }

    public enum Availability {
        IN_STOCK, OUT_OF_STOCK
    }
}
