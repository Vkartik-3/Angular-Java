package com.example.order.infrastructure.inbox;

import com.example.order.infrastructure.observability.OrderMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.time.Instant;

import static jakarta.transaction.Transactional.TxType.MANDATORY;

@ApplicationScoped
public class InboxService {

    @Inject
    InboxRepository repository;

    @Inject
    OrderMetrics metrics;

    @Transactional(MANDATORY)
    public boolean receive(String eventId, InboxEventType type) {
        try {
            repository.persist(
                    InboxEvent.builder()
                            .eventId(eventId)
                            .eventType(type)
                            .receivedAt(Instant.now())
                            .status(InboxEvent.Status.RECEIVED)
                            .build()
            );
            repository.flush();
            return true;
        } catch (PersistenceException e) {
            metrics.inboxDuplicate();
            return false;
        }
    }

    @Transactional(MANDATORY)
    public void markProcessed(String eventId) {
        InboxEvent event = repository.findById(eventId);
        event.setStatus(InboxEvent.Status.PROCESSED);
    }
}