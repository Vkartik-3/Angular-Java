package com.example.order.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

import static jakarta.transaction.Transactional.TxType.MANDATORY;

@ApplicationScoped
public class OutboxService {

    @Inject
    OutboxRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional(MANDATORY)
    public void save(String aggregateType, String aggregateId, OutboxEventType eventType, Object event) {
        try {
            OutboxEventEntity entity = OutboxEventEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(event))
                    .processed(false)
                    .build();
            repository.persist(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}