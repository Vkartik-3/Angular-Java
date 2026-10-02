package com.example.inventory.rest;

import com.example.inventory.infrastructure.messaging.InventoryEventConsumer;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/inventory")
@RolesAllowed("admin")
public class InventoryModeResource {

    @Inject
    InventoryEventConsumer consumer;

    @GET
    @Path("/mode")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean getMode() {
        return consumer.isAccepted();
    }

    @PUT
    @Path("/mode")
    public Response setMode(@QueryParam("accept") boolean accept) {
        consumer.setAccepted(accept);
        return Response.ok("Inventory acceptance set to: " + accept).build();
    }

    @GET
    @Path("/delay")
    @Produces(MediaType.APPLICATION_JSON)
    public int getDelay() {
        return consumer.getDelay();
    }

    @PUT
    @Path("/delay")
    public Response setDelay(@QueryParam("seconds") int seconds) {
        consumer.setDelay(seconds);
        return Response.ok("Inventory delay set to: " + seconds + "s").build();
    }

    @GET
    @Path("/crash")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean getCrash() {
        return consumer.isCrash();
    }

    @PUT
    @Path("/crash")
    public Response setCrash(@QueryParam("enabled") boolean enabled) {
        consumer.setCrash(enabled);
        return Response.ok("Inventory crash mode set to: " + enabled).build();
    }
}