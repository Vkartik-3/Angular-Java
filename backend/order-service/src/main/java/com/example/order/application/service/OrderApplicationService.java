package com.example.order.application.service;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.port.output.OrderHistoryRecorder;
import com.example.order.application.port.output.OrderRepository;
import com.example.order.domain.model.HistoryStatus;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;

import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class OrderApplicationService implements OrderUseCase {

    @Inject OrderRepository repository;
    @Inject OrderHistoryRecorder historyService;

    @Override
    public void create(Order order) {
        repository.save(order);
        historyService.record(order.getId().value(), HistoryStatus.from(order.getStatus()));
    }

    @Override
    public void pay(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.pay();
        repository.update(order);
        historyService.record(orderId.value(), HistoryStatus.from(order.getStatus()));
    }

    @Override
    public void approveInventory(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.approveInventory();
        repository.update(order);
        historyService.record(orderId.value(), HistoryStatus.from(order.getStatus()));
    }

    @Override
    public void failPayment(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.failPayment();
        repository.update(order);
        historyService.record(orderId.value(), HistoryStatus.from(order.getStatus()));
    }

    @Override
    public void rejectInventory(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.rejectInventory();
        repository.update(order);
        historyService.record(orderId.value(), HistoryStatus.from(order.getStatus()));
    }

    @Override
    public void cancel(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.cancel();
        repository.update(order);
        historyService.record(orderId.value(), HistoryStatus.from(order.getStatus()));
    }

    @Override
    public Order findById(OrderId id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id.value()));
    }

    @Override
    public List<Order> findAllOrders() {
        return repository.findAllOrders();
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }
}
