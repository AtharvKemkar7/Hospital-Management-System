package com.healthcare.medicalrecord.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Body of {@code POST /api/v1/medical-records/{id}/vitals}. All numeric
 * fields are optional — only the values that were actually measured
 * are recorded.
 */
public record CreateVitalRequest(

        @PastOrPresent
        Instant takenAt,

        @Min(30) @Max(300)
        Integer systolic,

        @Min(20) @Max(200)
        Integer diastolic,

        @Min(20) @Max(250)
        Integer heartRate,

        @DecimalMin("25.0") @DecimalMax("45.0")
        BigDecimal temperatureC,

        @Min(50) @Max(100)
        Integer spo2
) { }
