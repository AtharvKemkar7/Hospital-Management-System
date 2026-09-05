package com.healthcare.medicalrecord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of {@code POST /api/v1/medical-records/{id}/diagnoses}.
 *
 * <p>ICD-10 code is validated against the documented format
 * (letter, two digits, optional dot, optional 1-4 alphanumerics).
 * The same check is enforced at the database level by the
 * {@code ck_diagnoses_icd10_format} check constraint.
 */
public record CreateDiagnosisRequest(

        @NotBlank
        @Size(max = 16)
        @Pattern(regexp = "^[A-Z][0-9]{2}(\\.[0-9A-Z]{1,4})?$",
                 message = "icd10Code must be a valid ICD-10 code (e.g. 'I10', 'E11.9', 'S72.001A')")
        String icd10Code,

        @NotBlank
        @Size(max = 1000)
        String description,

        @Past
        LocalDate onsetDate
) { }
