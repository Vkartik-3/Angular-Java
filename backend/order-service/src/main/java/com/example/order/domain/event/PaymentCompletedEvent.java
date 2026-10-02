package com.example.order.domain.event;

import java.util.UUID;

public record PaymentCompletedEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {
    public static PaymentCompletedEvent of(UUID orderId, String correlationId) {
        return new PaymentCompletedEvent(UUID.randomUUID().toString(), orderId, correlationId);
    }
}