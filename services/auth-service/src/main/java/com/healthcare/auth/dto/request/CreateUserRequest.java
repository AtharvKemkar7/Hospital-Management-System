package com.healthcare.auth.dto.request;

import com.healthcare.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin-only payload for creating a user of any role. Submitted by an
 * authenticated administrator to {@code POST /api/v1/users}.
 */
public record CreateUserRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 12, max = 128)
        String password,

        @NotBlank
        @Size(min = 1, max = 100)
        String firstName,

        @NotBlank
        @Size(min = 1, max = 100)
        String lastName,

        @NotNull
        Role role
) { }
