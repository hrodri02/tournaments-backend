package com.example.tournaments_backend.instance;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stamps every response with the serving instance's id, so which backend
 * container handled a request is observable behind the nginx load balancer.
 */
public class InstanceIdFilter extends OncePerRequestFilter {

    public static final String DEFAULT_INSTANCE_ID = "default";

    private static final String HEADER_INSTANCE_ID = "X-Instance-Id";

    private final String instanceId;

    public InstanceIdFilter(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(HEADER_INSTANCE_ID, instanceId);
        filterChain.doFilter(request, response);
    }
}
