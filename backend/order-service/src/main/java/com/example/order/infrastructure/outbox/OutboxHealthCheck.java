package com.example.order.infrastructure.outbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class OutboxHealthCheck implements HealthCheck {

    @Inject
    OutboxRepository repository;

    @ConfigProperty(name = "app.outbox.max-retries")
    int maxRetries;

    @Override
    public HealthCheckResponse call() {
        long deadCount = repository.countDead(maxRetries);
        return HealthCheckResponse.named("outbox")
                .status(deadCount == 0)
                .withData("deadEvents", deadCount)
                .build();
    }
}