package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cross-cutting concern filter for API observability.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter to log
 * every incoming request and every outgoing response in a single class.
 *
 * This filter-based approach is superior to placing Logger.info() calls manually
 * inside every resource method because:
 *   1. Separation of concerns — resource methods focus purely on business logic.
 *   2. DRY principle — one location to change logging format for all endpoints.
 *   3. Cannot be forgotten — automatically applied to every registered endpoint.
 *   4. Can be toggled or extended globally without touching business code.
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %s %s",
            req.getMethod(),
            req.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d",
            req.getMethod(),
            req.getUriInfo().getRequestUri(),
            res.getStatus()));
    }
}
