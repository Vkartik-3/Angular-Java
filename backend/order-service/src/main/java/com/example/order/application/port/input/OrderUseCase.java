package com.example.order.application.port.input;

import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;

import java.util.List;
import java.util.UUID;

public interface OrderUseCase {
    void create(Order order);
    void pay(OrderId orderId);
    void approveInventory(OrderId orderId);
    void failPayment(OrderId orderId);
    void rejectInventory(OrderId orderId);
    void cancel(OrderId orderId);
    Order findById(OrderId id);
    List<Order> findAllOrders();
    List<Order> findByUserId(UUID userId);
}
