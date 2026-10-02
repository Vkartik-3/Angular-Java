package com.example.gateway.resource.mapper;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import com.example.gateway.resource.GatewayErrorResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProcessingExceptionMapper implements ExceptionMapper<ProcessingException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(ProcessingException exception) {
        Log.errorf(exception, "Downstream service unreachable [corrId=%s]", correlationIdProvider.get());
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new GatewayErrorResponse(502, "SERVICE_UNAVAILABLE",
                        "Downstream service temporarily unavailable", correlationIdProvider.get()))
                .build();
    }
}