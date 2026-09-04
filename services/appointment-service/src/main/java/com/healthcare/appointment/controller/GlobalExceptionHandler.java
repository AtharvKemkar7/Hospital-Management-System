package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.response.ErrorDetail;
import com.healthcare.appointment.dto.response.ErrorResponse;
import com.healthcare.appointment.exception.ApiException;
import com.healthcare.appointment.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> onApi(ApiException ex, HttpServletRequest req) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> onValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest req) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(),
                        fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
                .collect(Collectors.toList());
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request", req, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> onBadInput(HttpMessageNotReadableException ex,
                                                    HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "Malformed or missing request body", req, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> onAuth(AuthenticationException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Authentication is required to access this resource.", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> onForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to perform this action.", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> onIntegrity(DataIntegrityViolationException ex,
                                                      HttpServletRequest req) {
        log.warn("Data integrity violation on {} {}: {}",
                req.getMethod(), req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        // Map the (doctor_id, start_at) partial unique-index violation to a 409.
        return body(HttpStatus.CONFLICT, "APPOINTMENT_SLOT_CONFLICT",
                "Another active appointment already exists for this doctor at the requested time.",
                req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> onAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", req, List.of());
    }

    private ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
                                               HttpServletRequest req,
                                               List<ErrorDetail> details) {
        String corr = req.getHeader("X-Correlation-Id");
        if (corr == null) corr = MDC.get(CorrelationIdFilter.MDC_KEY);
        ErrorResponse er = ErrorResponse.of(status.value(), code, message,
                req.getRequestURI(), corr, details);
        return ResponseEntity.status(status).body(er);
    }
}
