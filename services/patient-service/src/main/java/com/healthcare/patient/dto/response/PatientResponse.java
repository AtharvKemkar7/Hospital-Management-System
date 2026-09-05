package com.healthcare.patient.dto.response;

import com.healthcare.patient.entity.Gender;
import com.healthcare.patient.entity.Patient;
import com.healthcare.patient.entity.PatientStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Safe projection of a {@link Patient} for API responses. Never includes
 * fields the caller is not authorized to see.
 */
public record PatientResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        PatientStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatientResponse from(Patient p) {
        return new PatientResponse(
                p.getId(),
                p.getUserId(),
                p.getFirstName(),
                p.getLastName(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getPhone(),
                p.getEmail(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
