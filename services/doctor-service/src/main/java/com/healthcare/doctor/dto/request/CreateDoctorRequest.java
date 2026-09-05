package com.healthcare.doctor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/doctors} (self-registration by an
 * authenticated DOCTOR). The {@code userId} is taken from the
 * authenticated principal; clients cannot supply it.
 */
public record CreateDoctorRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String firstName,

        @NotBlank
        @Size(min = 1, max = 100)
        String lastName,

        @NotBlank
        @Size(min = 3, max = 64)
        // License numbers vary by jurisdiction; allow alphanumerics, dashes,
        // dots, and slashes. We do not enforce a specific format here.
        @Pattern(regexp = "^[A-Za-z0-9.\\-/]+$",
                 message = "license number may only contain letters, digits, '.', '-', '/'")
        String licenseNumber,

        @NotBlank
        @Size(min = 1, max = 100)
        String specialty,

        @Size(max = 100)
        String subSpecialty,

        @Size(max = 100)
        String department,

        @Size(max = 32)
        String phone,

        @Email
        @Size(max = 320)
        String email
) { }
