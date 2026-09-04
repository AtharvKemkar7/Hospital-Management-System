package com.healthcare.auth.dto.response;

/**
 * Authentication response. Returned by /login, /refresh and /register.
 *
 * <p>The refresh token is the raw opaque value; only the hash is persisted
 * server-side. The client must store it securely.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long   expiresInSeconds,
        UserResponse user
) { }
