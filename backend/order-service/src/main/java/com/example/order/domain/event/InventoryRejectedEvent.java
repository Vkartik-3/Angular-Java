package com.example.order.domain.event;

import java.util.UUID;

public record InventoryRejectedEvent(
        String eventId,
        UUID orderId,
        String reason,
        String correlationId
) {
    public static InventoryRejectedEvent of(UUID orderId, String reason, String correlationId) {
        return new InventoryRejectedEvent(UUID.randomUUID().toString(), orderId, reason, correlationId);
    }
}