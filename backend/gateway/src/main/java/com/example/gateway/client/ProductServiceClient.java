package com.example.gateway.client;

import com.example.gateway.infrastructure.filter.OutgoingCorrelationIdFilter;
import com.example.gateway.infrastructure.filter.OutgoingJwtFilter;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

@RegisterRestClient(configKey = "product-service")
@RegisterProvider(OutgoingCorrelationIdFilter.class)
@RegisterProvider(OutgoingJwtFilter.class)
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public interface ProductServiceClient {

    @GET
    @Retry(delay = 200, abortOn = WebApplicationException.class)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response getAll();

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response importProducts(@BeanParam ProductImportForm form);

    class ProductImportForm {
        @RestForm("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;
    }
}
