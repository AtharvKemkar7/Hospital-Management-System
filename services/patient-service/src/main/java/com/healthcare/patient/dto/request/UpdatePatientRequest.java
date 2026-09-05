package com.healthcare.patient.dto.request;

import com.healthcare.patient.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of {@code PATCH /api/v1/patients/me}. All fields are optional;
 * only the supplied ones are applied.
 */
public record UpdatePatientRequest(

        @Size(min = 1, max = 100)
        String firstName,

        @Size(min = 1, max = 100)
        String lastName,

        @Past
        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 32)
        String phone,

        @Email
        @Size(max = 320)
        String email
) { }
