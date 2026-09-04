package com.healthcare.auth.service;

import com.healthcare.auth.config.JwtProperties;
import com.healthcare.auth.dto.request.CreateUserRequest;
import com.healthcare.auth.dto.request.LoginRequest;
import com.healthcare.auth.dto.request.RefreshRequest;
import com.healthcare.auth.dto.request.RegisterRequest;
import com.healthcare.auth.dto.response.AuthResponse;
import com.healthcare.auth.dto.response.UserResponse;
import com.healthcare.auth.entity.User;
import com.healthcare.auth.mapper.UserMapper;
import com.healthcare.auth.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the public auth use cases: register, login, refresh, logout.
 * Sits between the controller and the focused services (RegistrationService,
 * LoginService, CurrentUserService) and produces API-shaped responses.
 */
@Service
public class AuthService {

    private final RegistrationService registration;
    private final LoginService loginService;
    private final CurrentUserService currentUsers;
    private final UserMapper userMapper;
    private final JwtProperties jwtProps;

    public AuthService(RegistrationService registration,
                       LoginService loginService,
                       CurrentUserService currentUsers,
                       UserMapper userMapper,
                       JwtProperties jwtProps) {
        this.registration = registration;
        this.loginService = loginService;
        this.currentUsers = currentUsers;
        this.userMapper = userMapper;
        this.jwtProps = jwtProps;
    }

    public AuthResponse register(RegisterRequest req, String correlationId) {
        User user = registration.registerPublic(req, correlationId);
        // After registration, also issue tokens so the client can start
        // a session immediately (the foundation treats /register as
        // a public endpoint and does not require a separate login).
        LoginService.LoginResult result = loginService.login(
                user.getEmail(), req.password(), null, null);
        return toAuthResponse(result, /*correlationId*/ correlationId);
    }

    public AuthResponse login(LoginRequest req, HttpServletRequest http) {
        LoginService.LoginResult result = loginService.login(
                req.email(), req.password(), userAgent(http), remoteAddr(http));
        return toAuthResponse(result, correlationId(http));
    }

    public AuthResponse refresh(RefreshRequest req, HttpServletRequest http) {
        LoginService.RefreshResult result = loginService.refresh(
                req.refreshToken(), userAgent(http), remoteAddr(http));
        return toAuthResponse(result, correlationId(http));
    }

    public void logout() {
        loginService.logout(currentUsers.currentUserId());
    }

    public UserResponse currentUser() {
        return userMapper.toResponse(currentUsers.getCurrentUser());
    }

    public AuthResponse createUserAsAdmin(CreateUserRequest req, String correlationId) {
        User user = registration.createAsAdmin(req, correlationId);
        return new AuthResponse(
                /*accessToken*/ null,
                /*refreshToken*/ null,
                /*tokenType*/ null,
                /*expiresInSeconds*/ 0,
                userMapper.toResponse(user));
    }

    // ----------------------------------------------------------------- helpers

    private AuthResponse toAuthResponse(LoginService.LoginResult r, String correlationId) {
        return toAuthResponse(r.user(), r.access(), r.refresh(), correlationId);
    }

    private AuthResponse toAuthResponse(LoginService.RefreshResult r, String correlationId) {
        return toAuthResponse(r.user(), r.access(), r.refresh(), correlationId);
    }

    private AuthResponse toAuthResponse(User user,
                                        JwtTokenProvider.IssuedToken access,
                                        LoginService.IssuedRefresh refresh,
                                        String correlationId) {
        long expires = Math.max(0, access.expiresAt().getEpochSecond()
                - java.time.Instant.now().getEpochSecond());
        return new AuthResponse(
                access.token(),
                refresh.raw(),
                "Bearer",
                expires,
                userMapper.toResponse(user));
    }

    private String userAgent(HttpServletRequest req) {
        return req == null ? null : req.getHeader("User-Agent");
    }

    private String remoteAddr(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }

    private String correlationId(HttpServletRequest req) {
        return req == null ? null : req.getHeader("X-Correlation-Id");
    }

    /** Exposed for tests and admin endpoints that need the configured TTL. */
    public long accessTokenTtlSeconds() {
        return jwtProps.getAccessTokenExpirationSeconds();
    }
}
