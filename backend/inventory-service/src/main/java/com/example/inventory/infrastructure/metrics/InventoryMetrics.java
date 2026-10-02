package com.example.inventory.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InventoryMetrics {

    @Inject
    MeterRegistry registry;

    private Counter approved;
    private Counter rejected;

    @PostConstruct
    void init() {
        approved = registry.counter("inventory_requests_total", "result", "approved");
        rejected = registry.counter("inventory_requests_total", "result", "rejected");
    }

    public void approved() { approved.increment(); }
    public void rejected() { rejected.increment(); }
}