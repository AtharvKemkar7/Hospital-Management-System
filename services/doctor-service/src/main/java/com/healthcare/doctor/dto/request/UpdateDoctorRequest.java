package com.healthcare.doctor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PATCH /api/v1/doctors/me}. All fields are optional;
 * only the supplied ones are applied.
 */
public record UpdateDoctorRequest(

        @Size(min = 1, max = 100)
        String firstName,

        @Size(min = 1, max = 100)
        String lastName,

        @Size(min = 3, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9.\\-/]+$",
                 message = "license number may only contain letters, digits, '.', '-', '/'")
        String licenseNumber,

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
