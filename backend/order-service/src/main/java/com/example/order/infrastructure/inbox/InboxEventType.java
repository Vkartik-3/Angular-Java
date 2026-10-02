package com.example.order.infrastructure.inbox;

public enum InboxEventType {
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    INVENTORY_APPROVED,
    INVENTORY_REJECTED,
    PAYMENT_ROLLBACK_COMPLETED
}