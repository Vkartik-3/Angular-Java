package com.example.order.presentation.exception.mapper;

import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.presentation.exception.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(NotFoundException exception) {
        String correlationId = correlationIdProvider.get();
        Response.Status status = Response.Status.NOT_FOUND;
        String message = exception.getMessage() != null ? exception.getMessage() : "Resource not found";
        return Response.status(status)
                .entity(ErrorResponse.of(status.getStatusCode(), "NOT_FOUND", message, correlationId))
                .build();
    }
}