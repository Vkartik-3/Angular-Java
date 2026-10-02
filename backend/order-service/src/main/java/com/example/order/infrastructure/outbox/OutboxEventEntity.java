package com.example.order.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(columnList = "processed, retry_count"),
        @Index(columnList = "processed, retry_count, created_at")
})
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEventEntity {

    @Id
    private String id;

    @Column(name = "AGGREGATE_ID")
    private String aggregateId;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE")
    private OutboxEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "PROCESSED_AT")
    private Instant processedAt;

    private boolean processed;

    @Column(name = "RETRY_COUNT")
    private int retryCount;

    @Column(name = "LAST_ERROR", columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    void setCreationDate() {
        this.createdAt = Instant.now();
    }
}