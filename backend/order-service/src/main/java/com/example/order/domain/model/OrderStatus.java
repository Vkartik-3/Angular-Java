package com.example.order.domain.model;

public enum OrderStatus {
    CREATED,
    PAID,
    INVENTORY_APPROVED,
    PAYMENT_FAILED,
    INVENTORY_REJECTED,
    CANCELLED
}
