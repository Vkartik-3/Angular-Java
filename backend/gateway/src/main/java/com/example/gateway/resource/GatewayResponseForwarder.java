package com.example.gateway.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.util.Set;

@ApplicationScoped
public class GatewayResponseForwarder {

    private static final Set<String> BLOCKED_HEADERS = Set.of(
            "transfer-encoding", "content-length", "host", "connection", "x-correlation-id"
    );

    public Response forward(Response downstream) {
        String body = downstream.hasEntity() ? downstream.readEntity(String.class) : null;
        Response.ResponseBuilder builder = Response.status(downstream.getStatus());
        if (body != null) builder.entity(body);
        downstream.getHeaders().forEach((k, values) -> {
            if (!BLOCKED_HEADERS.contains(k.toLowerCase())) values.forEach(v -> builder.header(k, v));
        });
        return builder.build();
    }
}