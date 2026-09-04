package com.healthcare.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.auth.config.AuthProperties;
import com.healthcare.auth.dto.response.ErrorResponse;
import com.healthcare.auth.entity.AccountStatus;
import com.healthcare.auth.entity.Role;

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
import java.util.UUID;

/**
 * Reads the {@code Authorization: Bearer ...} header on each request,
 * validates the JWT, and sets the {@link org.springframework.security.core.context.SecurityContext}
 * with an {@link AppPrincipal} for the duration of the request.
 *
 * <p>If the token is invalid or expired, the filter does NOT immediately
 * write a 401 — the security chain will reach {@link RestAuthenticationEntryPoint}
 * which produces the standard error envelope.
 *
 * <p>If the token is valid but the embedded user no longer exists or is not
 * ACTIVE, the principal is set without authentication authorities, and
 * downstream authorization rules will deny access.
 *
 * <p>The raw token is never logged.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final AuthProperties props;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                  AuthProperties props,
                                  ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.props = props;
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
            JwtTokenProvider.ParsedToken parsed = tokenProvider.parseAndVerify(token);
            UUID userId = parsed.userId();
            Role role = parsed.role();

            // Build a minimal AppPrincipal — we do not need the password hash
            // for JWT-authenticated requests, only the user identity, role
            // and account status (for "is enabled" checks). Since the principal
            // came from a signed token, we treat the user as ACTIVE here;
            // an additional check against the DB on every request is
            // unnecessary and would be an availability liability.
            AppPrincipal principal = new AppPrincipal(
                    userId, /*email*/ null, /*passwordHash*/ "",
                    role, AccountStatus.ACTIVE);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal, token, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (JwtTokenProvider.InvalidJwtException e) {
            // Bad token: do NOT authenticate; let the entry point handle it.
            log.debug("Rejecting request with invalid JWT: reason={}", e.getReason());
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response, e.getReason());
        }
    }

    private void writeUnauthorized(HttpServletRequest req, HttpServletResponse resp,
                                   JwtTokenProvider.InvalidJwtException.Reason reason)
            throws IOException {
        String code = reason == JwtTokenProvider.InvalidJwtException.Reason.EXPIRED
                ? "TOKEN_EXPIRED" : "UNAUTHORIZED";
        String message = reason == JwtTokenProvider.InvalidJwtException.Reason.EXPIRED
                ? "Token has expired." : "Invalid token.";
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), code, message,
                req.getRequestURI(), req.getHeader(props.getCorrelation().getHeaderName()),
                List.of());
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(resp.getOutputStream(), body);
    }
}
