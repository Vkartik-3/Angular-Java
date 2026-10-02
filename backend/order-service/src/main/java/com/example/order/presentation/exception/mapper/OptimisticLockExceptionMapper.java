package com.example.order.presentation.exception.mapper;

import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.presentation.exception.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(OptimisticLockException exception) {
        String correlationId = correlationIdProvider.get();
        Response.Status status = Response.Status.CONFLICT;
        return Response.status(status)
                .entity(ErrorResponse.of(status.getStatusCode(), "CONCURRENT_MODIFICATION",
                        "Resource was modified concurrently. Please retry.", correlationId))
                .build();
    }
}