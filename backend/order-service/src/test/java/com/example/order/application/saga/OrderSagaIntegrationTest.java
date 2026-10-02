package com.example.order.application.saga;

import com.example.order.domain.event.InventoryApprovedEvent;
import com.example.order.domain.event.InventoryRejectedEvent;
import com.example.order.domain.event.PaymentCompletedEvent;
import com.example.order.domain.event.PaymentFailedEvent;
import com.example.order.domain.event.PaymentRollbackCompletedEvent;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OrderSagaIntegrationTest {

    static final String CUSTOMER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Inject OrderSagaOrchestrator orchestrator;
    @Inject OrderSagaRepository sagaRepository;
    @Inject OutboxRepository outboxRepository;
    @Inject SagaTimeoutJob timeoutJob;

    private String token;

    @BeforeEach
    void generateToken() {
        token = Jwt.claims()
                .subject(CUSTOMER_ID)
                .claim("preferred_username", "test-customer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .sign();
    }

    @Test
    void happyPath_orderCompletedAfterPaymentAndInventoryApproval() {
        UUID orderId = createOrder();
        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_PAYMENT);

        orchestrator.onPaymentCompleted(PaymentCompletedEvent.of(orderId, "corr-1"));

        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);
        assertTrue(
                outboxRepository.count("eventType = ?1 and aggregateId = ?2",
                        OutboxEventType.INVENTORY_REQUEST, orderId.toString()) > 0,
                "Expected INVENTORY_REQUEST outbox entry");

        orchestrator.onInventoryApproved(InventoryApprovedEvent.of(orderId, "corr-1"));

        assertSagaStep(orderId, OrderSagaState.SagaStep.COMPLETED);
        assertOrderStatus(orderId, "INVENTORY_APPROVED");
    }

    @Test
    void paymentFailed_sagaCancelledAndOrderMarkedPaymentFailed() {
        UUID orderId = createOrder();

        orchestrator.onPaymentFailed(PaymentFailedEvent.of(orderId, "Insufficient funds", "corr-1"));

        assertSagaStep(orderId, OrderSagaState.SagaStep.CANCELLED);
        assertOrderStatus(orderId, "PAYMENT_FAILED");
    }

    @Test
    void inboxIdempotency_duplicateEventProcessedOnlyOnce() {
        UUID orderId = createOrder();
        PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, "corr-1");

        orchestrator.onPaymentCompleted(event);
        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);

        // same eventId — inbox must block reprocessing
        orchestrator.onPaymentCompleted(event);
        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);
    }

    @Test
    void inventoryRejected_compensationFlowCompletesWithPaymentRollback() {
        UUID orderId = createOrder();
        orchestrator.onPaymentCompleted(PaymentCompletedEvent.of(orderId, "corr-1"));
        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);

        orchestrator.onInventoryRejected(InventoryRejectedEvent.of(orderId, "Out of stock", "corr-1"));

        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_ROLLBACK);
        assertTrue(
                outboxRepository.count("eventType = ?1 and aggregateId = ?2",
                        OutboxEventType.PAYMENT_ROLLBACK, orderId.toString()) > 0,
                "Expected PAYMENT_ROLLBACK outbox entry after inventory rejection");
        assertOrderStatus(orderId, "INVENTORY_REJECTED");

        orchestrator.onPaymentRolledBack(new PaymentRollbackCompletedEvent(
                UUID.randomUUID().toString(), orderId, "corr-1"));

        assertSagaStep(orderId, OrderSagaState.SagaStep.CANCELLED);
        assertOrderStatus(orderId, "INVENTORY_REJECTED");
    }

    @Test
    void sagaTimeout_waitingInventory_cancelsOrderAndPublishesPaymentRollback() {
        UUID orderId = createOrder();
        orchestrator.onPaymentCompleted(PaymentCompletedEvent.of(orderId, "corr-1"));
        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);

        expireSaga(orderId);
        timeoutJob.checkTimeouts();

        assertSagaStep(orderId, OrderSagaState.SagaStep.WAITING_ROLLBACK);
        assertTrue(
                outboxRepository.count("eventType = ?1 and aggregateId = ?2",
                        OutboxEventType.PAYMENT_ROLLBACK, orderId.toString()) > 0,
                "Expected PAYMENT_ROLLBACK outbox entry after inventory timeout");
        assertOrderStatus(orderId, "CANCELLED");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private UUID createOrder() {
        String location = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {"items":[{"productId":"%s","quantity":2,"price":34.99}]}
                        """.formatted(UUID.randomUUID()))
                .when().post("/orders")
                .then().statusCode(202)
                .extract().header("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private void assertOrderStatus(UUID orderId, String expectedStatus) {
        given()
                .auth().oauth2(token)
                .when().get("/orders/" + orderId)
                .then().statusCode(200)
                .body("status", equalTo(expectedStatus));
    }

    @Transactional
    void expireSaga(UUID orderId) {
        OrderSagaState saga = sagaRepository.findById(orderId);
        saga.setDeadline(Instant.now().minusSeconds(60));
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void assertSagaStep(UUID orderId, OrderSagaState.SagaStep expected) {
        OrderSagaState saga = sagaRepository.findById(orderId);
        assertNotNull(saga, "Saga not found for orderId=" + orderId);
        assertEquals(expected, saga.getStep());
    }
}