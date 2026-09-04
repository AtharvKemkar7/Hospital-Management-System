package com.healthcare.auth.controller;

import com.healthcare.auth.dto.request.CreateUserRequest;
import com.healthcare.auth.dto.response.AuthResponse;
import com.healthcare.auth.service.AuthService;
import com.healthcare.auth.web.CorrelationIdFilter;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user management. Only callers with the
 * {@code ADMIN} authority can create or modify users with privileged
 * roles.
 *
 * <p>Phase 1 implements the create endpoint. List, get, status change,
 * and role assignment endpoints will be added in later phases.
 */
@RestController
@RequestMapping("/api/v1/users")
public class AdminUserController {

    private final AuthService auth;

    public AdminUserController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        String corr = MDC.get(CorrelationIdFilter.MDC_KEY);
        AuthResponse resp = auth.createUserAsAdmin(req, corr);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}
