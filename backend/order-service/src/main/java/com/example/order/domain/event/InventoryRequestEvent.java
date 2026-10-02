package com.example.order.domain.event;

import java.util.UUID;

public record InventoryRequestEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {
    public static InventoryRequestEvent of(UUID orderId, String correlationId) {
        return new InventoryRequestEvent(UUID.randomUUID().toString(), orderId, correlationId);
    }
}
