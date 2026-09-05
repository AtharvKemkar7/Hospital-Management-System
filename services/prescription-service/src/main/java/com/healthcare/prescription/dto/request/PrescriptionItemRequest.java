package com.healthcare.prescription.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/prescriptions/{id}/items}, or part of
 * {@code CreatePrescriptionRequest#items}.
 *
 * <p>Required fields are {@code drugName}, {@code dosage}, and
 * {@code frequency}. Route, duration, quantity, and instructions are
 * optional. Length limits match the column lengths in
 * {@code V1__create_prescriptions.sql}.
 */
public record PrescriptionItemRequest(

        @NotBlank
        @Size(min = 1, max = 200)
        String drugName,

        @NotBlank
        @Size(min = 1, max = 100)
        String dosage,

        @NotBlank
        @Size(min = 1, max = 100)
        String frequency,

        @Size(max = 50)
        String route,

        @Min(1) @Max(365)
        Integer durationDays,

        @Min(1) @Max(1000)
        Integer quantity,

        @Size(max = 1000)
        String instructions
) { }
