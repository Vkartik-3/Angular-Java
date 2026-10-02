package com.example.order.infrastructure.outbox;

import com.example.order.domain.event.PaymentRequestEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.domain.event.InventoryRequestEvent;
import com.example.order.infrastructure.messaging.KafkaOrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.MDC;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class OutboxPublisherJob {

    @Inject
    OutboxRepository repository;

    @Inject
    KafkaOrderEventPublisher kafka;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.outbox.batch-size")
    int batchSize;

    @ConfigProperty(name = "app.outbox.max-retries")
    int maxRetries;

    @ConfigProperty(name = "app.outbox.retention-days")
    int retentionDays;

    @Scheduled(every = "5s")
    @Transactional
    public void publish() {
        var pending = repository.findUnprocessed(batchSize, maxRetries);
        pending.forEach(this::processPublish);
    }

    void processPublish(OutboxEventEntity event) {
        MDC.put("orderId", event.getAggregateId());
        try {
            route(event);
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            Log.infof("Outbox sent type=%s", event.getEventType());
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(e.getMessage());
        } finally {
            MDC.remove("orderId");
        }
    }

    private void route(OutboxEventEntity event) throws Exception {
        switch (event.getEventType()) {
            case PAYMENT_REQUEST -> {
                var payload = objectMapper.readValue(event.getPayload(), PaymentRequestEvent.class);
                kafka.publishPaymentRequest(payload);
            }
            case INVENTORY_REQUEST -> {
                var payload = objectMapper.readValue(event.getPayload(), InventoryRequestEvent.class);
                kafka.publishInventoryRequest(payload);
            }
            case PAYMENT_ROLLBACK -> {
                var payload = objectMapper.readValue(event.getPayload(), PaymentRollbackEvent.class);
                kafka.publishPaymentRollback(payload);
            }
        }
    }

    @Scheduled(every = "1m")
    public void logDeadEvents() {
        var dead = repository.findDead(maxRetries);
        dead.forEach(e -> Log.errorf("DEAD OUTBOX EVENT id=%s error=%s", e.getId(), e.getLastError()));
    }

    @Scheduled(every = "1h")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        repository.deleteProcessed(cutoff);
    }
}