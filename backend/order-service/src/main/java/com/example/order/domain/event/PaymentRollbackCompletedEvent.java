package com.example.order.domain.event;

import java.util.UUID;

public record PaymentRollbackCompletedEvent(
        String eventId,
        UUID orderId,
        String correlationId
) {}
