package com.healthcare.doctor.dto.response;

import com.healthcare.doctor.entity.Doctor;
import com.healthcare.doctor.entity.DoctorStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe projection of a {@link Doctor} for API responses. Never includes
 * fields the caller is not authorized to see.
 */
public record DoctorResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String licenseNumber,
        String specialty,
        String subSpecialty,
        String department,
        String phone,
        String email,
        DoctorStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static DoctorResponse from(Doctor d) {
        return new DoctorResponse(
                d.getId(),
                d.getUserId(),
                d.getFirstName(),
                d.getLastName(),
                d.getLicenseNumber(),
                d.getSpecialty(),
                d.getSubSpecialty(),
                d.getDepartment(),
                d.getPhone(),
                d.getEmail(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
