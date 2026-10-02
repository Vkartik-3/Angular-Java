package com.example.order.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestEvent(
        String eventId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String correlationId
) {
    public static PaymentRequestEvent of(UUID orderId, UUID customerId, BigDecimal amount, String correlationId) {
        return new PaymentRequestEvent(UUID.randomUUID().toString(), orderId, customerId, amount, correlationId);
    }
}
