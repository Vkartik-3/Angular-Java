package com.example.order.domain.event;

import java.util.UUID;

public record InventoryApprovedEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {
    public static InventoryApprovedEvent of(UUID orderId, String correlationId) {
        return new InventoryApprovedEvent(UUID.randomUUID().toString(), orderId, correlationId);
    }
}