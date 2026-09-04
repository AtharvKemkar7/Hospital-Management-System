package com.healthcare.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.auth.config.AuthProperties;
import com.healthcare.auth.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Translates authentication failures (missing or invalid JWT) into the
 * standard error envelope. Used by Spring Security when an
 * {@link AuthenticationException} bubbles up to the entry point.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final AuthProperties props;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, AuthProperties props) {
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication is required to access this resource.",
                request.getRequestURI(),
                request.getHeader(props.getCorrelation().getHeaderName()),
                List.of());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
