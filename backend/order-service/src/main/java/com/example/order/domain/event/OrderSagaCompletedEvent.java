package com.example.order.domain.event;

import java.util.UUID;

public record OrderSagaCompletedEvent(UUID orderId) {}