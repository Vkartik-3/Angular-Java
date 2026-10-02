package com.example.order.domain.model;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(new OrderId(UUID.randomUUID()));
    }

    @Test
    void newOrderHasCreatedStatus() {
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void addItemIncreasesItemCount() {
        order.addItem(UUID.randomUUID().toString(), 2, new BigDecimal("34.99"), null, null);
        assertEquals(1, order.getItems().size());
    }

    @Test
    void addItemWithZeroQuantityThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> order.addItem(UUID.randomUUID().toString(), 0, new BigDecimal("34.99"), null, null));
    }

    @Test
    void addItemWithNegativeQuantityThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> order.addItem(UUID.randomUUID().toString(), -1, new BigDecimal("34.99"), null, null));
    }

    @Test
    void totalAmountSumsAllItems() {
        order.addItem(UUID.randomUUID().toString(), 2, new BigDecimal("34.99"), null, null);
        order.addItem(UUID.randomUUID().toString(), 1, new BigDecimal("29.99"), null, null);

        Money total = order.totalAmount();

        assertEquals(new BigDecimal("99.97"), total.amount());
    }

    @Test
    void totalAmountOnEmptyOrderThrows() {
        assertThrows(IllegalStateException.class, () -> order.totalAmount());
    }

    @Test
    void payTransitionsToPaid() {
        order.pay();
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void payFromInvalidStateThrows() {
        order.pay();
        assertThrows(IllegalStateException.class, () -> order.pay());
    }

    @Test
    void payOnCancelledOrderIsNoOp() {
        order.cancel();
        order.pay();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void cancelTransitionsToCancelled() {
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void cancelIsIdempotent() {
        order.cancel();
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void failPaymentTransitionsToPaymentFailed() {
        order.failPayment();
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
    }

    @Test
    void approveInventoryFromPaidTransitionsToApproved() {
        order.pay();
        order.approveInventory();
        assertEquals(OrderStatus.INVENTORY_APPROVED, order.getStatus());
    }

    @Test
    void approveInventoryFromWrongStateThrows() {
        assertThrows(IllegalStateException.class, () -> order.approveInventory());
    }

    @Test
    void approveInventoryOnCancelledOrderIsNoOp() {
        order.cancel();
        order.approveInventory();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
}
