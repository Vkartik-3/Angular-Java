package com.example.order.domain.model;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Order {

    private final OrderId id;
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;
    private UUID idempotencyKey;
    private UUID userId;
    private String username;
    private Instant createdAt;

    public Order(OrderId id) {
        this.id = id;
        this.status = OrderStatus.CREATED;
    }

    public static Order reconstitute(OrderId id, List<OrderItem> items, OrderStatus status, UUID idempotencyKey, UUID userId, String username, Instant createdAt) {
        Order order = new Order(id);
        order.items = new ArrayList<>(items);
        order.status = status;
        order.idempotencyKey = idempotencyKey;
        order.userId = userId;
        order.username = username;
        order.createdAt = createdAt;
        return order;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void addItem(String productId, int quantity, BigDecimal price, String productName, String imageUrl) {
        if (quantity <= 0) throw new IllegalArgumentException();
        items.add(new OrderItem(UUID.randomUUID(), this.id, productId, quantity, new Money(price), productName, imageUrl));
    }

    public Money totalAmount() {
        if (items.isEmpty()) throw new IllegalStateException("Order has no items");
        return items.stream()
                .map(i -> i.price().multiply(i.quantity()))
                .reduce(Money.ZERO, Money::add);
    }

    public void pay() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.CREATED) throw new IllegalStateException("Invalid state transition to PAID from " + status);
        this.status = OrderStatus.PAID;
    }

    public void approveInventory() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.PAID) throw new IllegalStateException("Invalid state transition to INVENTORY_APPROVED from " + status);
        this.status = OrderStatus.INVENTORY_APPROVED;
    }

    public void failPayment() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.CREATED) throw new IllegalStateException("Invalid state transition to PAYMENT_FAILED from " + status);
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void rejectInventory() {
        if (status == OrderStatus.CANCELLED) return;
        if (status != OrderStatus.PAID) throw new IllegalStateException("Invalid state transition to INVENTORY_REJECTED from " + status);
        this.status = OrderStatus.INVENTORY_REJECTED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) return;
        this.status = OrderStatus.CANCELLED;
    }
}
