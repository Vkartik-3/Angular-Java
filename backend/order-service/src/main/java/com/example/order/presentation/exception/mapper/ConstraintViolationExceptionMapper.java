package com.example.order.presentation.exception.mapper;

import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.presentation.exception.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String correlationId = correlationIdProvider.get();
        Response.Status status = Response.Status.BAD_REQUEST;
        List<String> errors = exception.getConstraintViolations().stream()
                .map(v -> {
                    String field = v.getPropertyPath().toString();
                    field = field.substring(field.lastIndexOf('.') + 1);
                    return field + ": " + v.getMessage();
                })
                .toList();
        return Response.status(status)
                .entity(ErrorResponse.of(status.getStatusCode(), "VALIDATION_ERROR", "Validation failed", correlationId, errors))
                .build();
    }
}