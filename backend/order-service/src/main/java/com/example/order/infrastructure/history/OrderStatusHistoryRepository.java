package com.example.order.infrastructure.history;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderStatusHistoryRepository implements PanacheRepositoryBase<OrderStatusHistory, UUID> {

    public List<OrderStatusHistory> findByOrderId(UUID orderId) {
        return find("orderId = ?1 order by occurredAt asc", orderId).list();
    }
}
