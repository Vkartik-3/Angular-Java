package com.example.order.domain.event;

import java.util.UUID;

public record PaymentRollbackEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {
    public static PaymentRollbackEvent of(UUID orderId, String correlationId) {
        return new PaymentRollbackEvent(UUID.randomUUID().toString(), orderId, correlationId);
    }
}