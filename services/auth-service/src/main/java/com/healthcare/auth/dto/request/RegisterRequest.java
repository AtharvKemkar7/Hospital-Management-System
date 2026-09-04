package com.healthcare.auth.dto.request;

import com.healthcare.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload.
 *
 * <p>For Phase 1 the {@code role} field, if present, is <b>ignored</b>:
 * public registration always creates a {@code PATIENT} account. Privileged
 * accounts (DOCTOR, ADMIN, RECEPTIONIST, BILLING_STAFF) must be created
 * through the admin-only {@code POST /api/v1/users} endpoint.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 12, max = 128,
              message = "password must be between 12 and 128 characters")
        String password,

        @NotBlank
        @Size(min = 1, max = 100)
        String firstName,

        @NotBlank
        @Size(min = 1, max = 100)
        String lastName,

        // Present for forward-compat with the API; never honored by the
        // public registration flow. See AuthService#register.
        Role role
) { }
