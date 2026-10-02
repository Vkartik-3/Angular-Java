package com.example.order.application.saga;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_saga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSagaState {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStep step;

    @Column(nullable = false)
    private Instant deadline;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SagaStep {
        WAITING_PAYMENT,
        WAITING_INVENTORY,
        WAITING_ROLLBACK,
        COMPLETED,
        CANCELLED
    }

}
