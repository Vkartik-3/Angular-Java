package com.example.order.presentation;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.history.OrderStatusHistoryService;
import com.example.order.infrastructure.sse.OrderSseService;
import com.example.order.presentation.dto.CreateOrderRequest;
import com.example.order.presentation.dto.OrderResponse;
import com.example.order.presentation.dto.StatusHistoryEntryResponse;
import com.example.order.presentation.mapper.OrderPresentationMapper;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.MDC;
import io.smallrye.mutiny.Multi;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/orders")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderUseCase service;

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    OrderPresentationMapper mapper;

    @Inject
    OrderStatusHistoryService historyService;

    @Inject
    OrderSseService sseService;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "app.roles.admin")
    String adminRole;

    @POST
    @Retry(delay = 500, retryOn = OptimisticLockException.class)
    public Response create(@Valid CreateOrderRequest request,
                           @HeaderParam("Idempotency-Key") UUID idempotencyKey) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaim("preferred_username");
        Log.infof("Order received customerId=%s username=%s", customerId, username);
        UUID orderId = orchestrator.start(request, customerId, username, idempotencyKey);
        Response.ResponseBuilder builder = Response.accepted()
                .header("Location", "/orders/" + orderId);
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return builder.build();
    }

    @PUT
    @Path("/{id}/cancel")
    @Retry(delay = 200, retryOn = OptimisticLockException.class, abortOn = NotFoundException.class)
    public Response cancel(@PathParam("id") UUID id) {
        MDC.put("orderId", id.toString());
        Log.infof("Order cancel requested");
        Order existing = service.findById(new OrderId(id));
        if (!jwt.getGroups().contains(adminRole) && !UUID.fromString(jwt.getSubject()).equals(existing.getUserId())) {
            throw new ForbiddenException();
        }
        orchestrator.cancelByUser(new OrderId(id));
        Order order = service.findById(new OrderId(id));
        return Response.ok(mapper.toResponse(order)).build();
    }

    @GET
    @Retry(delay = 200, retryOn = PersistenceException.class)
    public List<OrderResponse> getAll() {
        if (jwt.getGroups().contains(adminRole)) {
            return service.findAllOrders().stream().map(mapper::toResponse).toList();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return service.findByUserId(userId).stream().map(mapper::toResponse).toList();
    }

    @GET
    @Path("/{id}")
    @Retry(delay = 200, retryOn = PersistenceException.class, abortOn = NotFoundException.class)
    public OrderResponse getById(@PathParam("id") UUID id) {
        Order order = service.findById(new OrderId(id));
        if (!jwt.getGroups().contains(adminRole) && !UUID.fromString(jwt.getSubject()).equals(order.getUserId())) {
            throw new ForbiddenException();
        }
        List<StatusHistoryEntryResponse> history = historyService.findByOrderId(id).stream()
                .map(h -> new StatusHistoryEntryResponse(h.getStatus().name(), h.getOccurredAt()))
                .toList();
        return mapper.toResponse(order, history);
    }

    @GET
    @Path("/{id}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Blocking
    public Multi<String> streamStatus(@PathParam("id") UUID id) {
        Order order = service.findById(new OrderId(id));
        if (!jwt.getGroups().contains(adminRole) && !UUID.fromString(jwt.getSubject()).equals(order.getUserId())) {
            throw new ForbiddenException();
        }
        return sseService.stream(id);
    }
}