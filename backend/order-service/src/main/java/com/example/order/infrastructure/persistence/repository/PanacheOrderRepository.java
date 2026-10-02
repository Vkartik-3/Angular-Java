package com.example.order.infrastructure.persistence.repository;

import com.example.order.application.port.output.OrderRepository;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.mapper.OrderMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheOrderRepository implements OrderRepository, PanacheRepositoryBase<OrderEntity, UUID> {

    @Inject
    OrderMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        return findByIdOptional(id.value()).map(mapper::toDomain);
    }

    @Override
    public void save(Order order) {
        persist(mapper.toEntity(order, new OrderEntity()));
    }

    @Override
    public List<Order> findAllOrders() {
        return listAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return find("userId", userId).list().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Optional<Order> findByIdempotencyKey(UUID idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional().map(mapper::toDomain);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public void update(Order order) {
        OrderEntity managed = findByIdOptional(order.getId().value())
                .orElseThrow(() -> new NotFoundException("Order not found: " + order.getId().value()));
        mapper.toEntity(order, managed);
    }
}