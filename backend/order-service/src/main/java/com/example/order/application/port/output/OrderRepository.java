package com.example.order.application.port.output;

import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
    Optional<Order> findByIdempotencyKey(UUID idempotencyKey);
    void update(Order order);
    List<Order> findAllOrders();
    List<Order> findByUserId(UUID userId);
}