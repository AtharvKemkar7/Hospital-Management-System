package com.healthcare.auth.dto.response;

import com.healthcare.auth.entity.AccountStatus;
import com.healthcare.auth.entity.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe projection of a {@link com.healthcare.auth.entity.User} for API
 * responses. Never includes the password hash, failed-login counter, or
 * lockout timestamps.
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        AccountStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt
) { }
