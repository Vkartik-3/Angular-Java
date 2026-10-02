package com.example.order.infrastructure.outbox;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class OutboxRepository implements PanacheRepository<OutboxEventEntity> {

    @Inject
    EntityManager em;

    @SuppressWarnings("unchecked")
    public List<OutboxEventEntity> findUnprocessed(int limit, int maxRetries) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
        return em.createNativeQuery("""
                SELECT * FROM outbox_events
                WHERE processed = false
                  AND retry_count < :maxRetries
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT :limit
                """, OutboxEventEntity.class)
                .setParameter("limit", limit)
                .setParameter("maxRetries", maxRetries)
                .getResultList();
    }

    public List<OutboxEventEntity> findDead(int maxRetries) {
        return find("retryCount >= ?1 and processed = false", maxRetries).list();
    }

    public long countDead(int maxRetries) {
        return count("retryCount >= ?1 and processed = false", maxRetries);
    }

    public long countPending(int maxRetries) {
        return count("processed = false and retryCount < ?1", maxRetries);
    }

    public void deleteProcessed(Instant cutoff) {
        delete("(processed = true and processedAt < ?1) or (processed = false and createdAt < ?1)", cutoff);
    }
}