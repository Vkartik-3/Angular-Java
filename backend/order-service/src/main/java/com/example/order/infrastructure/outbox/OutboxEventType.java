package com.example.order.infrastructure.outbox;

public enum OutboxEventType {
    PAYMENT_REQUEST,
    INVENTORY_REQUEST,
    PAYMENT_ROLLBACK
}