package com.example.order.application.saga;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class SagaCleanupJob {

    @ConfigProperty(name = "app.saga.retention-days", defaultValue = "3")
    int retentionDays;

    @Inject
    OrderSagaRepository sagaRepository;

    @Scheduled(every = "1h")
    @Transactional
    void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        long deleted = sagaRepository.deleteCompletedOlderThan(cutoff);
        if (deleted > 0) {
            Log.infof("Deleted %d completed saga records", deleted);
        }
    }
}
