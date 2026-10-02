package com.example.order.domain.model;

public enum HistoryStatus {
    CREATED, PAID, INVENTORY_APPROVED, PAYMENT_FAILED, INVENTORY_REJECTED, CANCELLED, PAYMENT_ROLLED_BACK;

    public static HistoryStatus from(OrderStatus status) {
        return valueOf(status.name());
    }
}