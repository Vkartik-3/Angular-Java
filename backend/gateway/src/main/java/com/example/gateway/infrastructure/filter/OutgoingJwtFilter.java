package com.example.gateway.infrastructure.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class OutgoingJwtFilter implements ClientRequestFilter {

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ClientRequestContext ctx) {
        String raw = jwt.getRawToken();
        if (raw != null) {
            ctx.getHeaders().putSingle("Authorization", "Bearer " + raw);
        }
    }
}
