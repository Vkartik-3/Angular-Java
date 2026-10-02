package com.example.order.application.saga;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderSagaRepository implements PanacheRepositoryBase<OrderSagaState, UUID> {

    @Inject
    EntityManager em;

    @SuppressWarnings("unchecked")
    public List<OrderSagaState> findExpired(Instant now) {
        return em.createNativeQuery("""
                SELECT * FROM order_saga
                WHERE deadline < :now
                  AND step IN ('WAITING_PAYMENT', 'WAITING_INVENTORY', 'WAITING_ROLLBACK')
                ORDER BY deadline
                FOR UPDATE SKIP LOCKED
                """, OrderSagaState.class)
                .setParameter("now", now)
                .getResultList();
    }

    public long deleteCompletedOlderThan(Instant cutoff) {
        return delete(
                "step in (?1, ?2) and updatedAt < ?3",
                OrderSagaState.SagaStep.COMPLETED,
                OrderSagaState.SagaStep.CANCELLED,
                cutoff
        );
    }
}
