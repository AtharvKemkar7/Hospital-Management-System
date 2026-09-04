package com.healthcare.appointment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.appointment.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtTokenVerifier verifier;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenVerifier verifier, ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            JwtTokenVerifier.ParsedToken parsed = verifier.parseAndVerify(token);
            AppointmentPrincipal principal = new AppointmentPrincipal(parsed.userId(), parsed.role());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal, token, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (JwtTokenVerifier.InvalidJwtException e) {
            log.debug("Rejecting request with invalid JWT: reason={}", e.getReason());
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response, e.getReason());
        }
    }

    private void writeUnauthorized(HttpServletRequest req, HttpServletResponse resp,
                                   JwtTokenVerifier.InvalidJwtException.Reason reason)
            throws IOException {
        String code = reason == JwtTokenVerifier.InvalidJwtException.Reason.EXPIRED
                ? "TOKEN_EXPIRED" : "UNAUTHORIZED";
        String message = reason == JwtTokenVerifier.InvalidJwtException.Reason.EXPIRED
                ? "Token has expired." : "Invalid token.";
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), code, message,
                req.getRequestURI(),
                req.getHeader("X-Correlation-Id"),
                List.of());
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(resp.getOutputStream(), body);
    }
}
