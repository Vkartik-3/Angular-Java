package com.example.order.infrastructure.observability;

import com.example.order.infrastructure.outbox.OutboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OutboxMetricsJob {

    @Inject OutboxRepository outboxRepository;
    @Inject OrderMetrics metrics;

    @ConfigProperty(name = "app.outbox.max-retries")
    int maxRetries;

    @Scheduled(every = "15s")
    @Transactional
    void refresh() {
        metrics.updateOutboxPending(outboxRepository.countPending(maxRetries));
    }
}