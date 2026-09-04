package com.healthcare.auth.controller;

import com.healthcare.auth.dto.request.LoginRequest;
import com.healthcare.auth.dto.request.RefreshRequest;
import com.healthcare.auth.dto.request.RegisterRequest;
import com.healthcare.auth.dto.response.AuthResponse;
import com.healthcare.auth.dto.response.UserResponse;
import com.healthcare.auth.service.AuthService;
import com.healthcare.auth.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    // Public -----------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse resp = auth.register(req, correlationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req,
                              HttpServletRequest http) {
        return auth.login(req, http);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req,
                                HttpServletRequest http) {
        return auth.refresh(req, http);
    }

    // Authenticated ----------------------------------------------------

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        auth.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return auth.currentUser();
    }

    private static String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
