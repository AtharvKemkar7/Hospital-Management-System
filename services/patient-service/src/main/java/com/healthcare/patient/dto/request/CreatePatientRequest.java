package com.healthcare.patient.dto.request;

import com.healthcare.patient.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of {@code POST /api/v1/patients} (self-registration by an
 * authenticated PATIENT). The {@code userId} is taken from the
 * authenticated principal; clients cannot supply it.
 */
public record CreatePatientRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String firstName,

        @NotBlank
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
