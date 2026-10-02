package com.example.order.domain.event;

import java.util.UUID;

public record PaymentFailedEvent(
        String eventId,
        UUID orderId,
        String reason,
        String correlationId
) {
    public static PaymentFailedEvent of(UUID orderId, String reason, String correlationId) {
        return new PaymentFailedEvent(UUID.randomUUID().toString(), orderId, reason, correlationId);
    }
}