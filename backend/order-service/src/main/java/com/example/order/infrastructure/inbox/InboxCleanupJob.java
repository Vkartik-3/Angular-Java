package com.example.order.infrastructure.inbox;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class InboxCleanupJob {

    @Inject
    InboxRepository repository;

    @ConfigProperty(name = "app.inbox.retention-days")
    int retentionDays;

    @Scheduled(every = "1h")
    @Transactional
    void cleanup() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            Log.infof("Inbox cleanup removed %d records", deleted);
        }
    }
}
