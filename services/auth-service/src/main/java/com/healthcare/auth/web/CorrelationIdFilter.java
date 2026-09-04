package com.healthcare.auth.web;

import com.healthcare.auth.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads (or generates) a correlation ID for every request, places it in
 * the response header and in the SLF4J MDC, so every log line written
 * during the request carries it.
 *
 * <p>Correlation IDs are random UUIDs: they contain no sensitive data and
 * are safe to log in full.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "correlationId";

    private final String headerName;

    public CorrelationIdFilter(AuthProperties props) {
        this.headerName = props.getCorrelation().getHeaderName();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(headerName);
        if (id == null || id.isBlank() || id.length() > 128) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        response.setHeader(headerName, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
