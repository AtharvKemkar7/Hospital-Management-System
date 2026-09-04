package com.healthcare.appointment.dto.response;

import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.AppointmentStatus;
import com.healthcare.appointment.entity.AppointmentType;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID doctorId,
        Instant startAt,
        Instant endAt,
        AppointmentType type,
        String reason,
        AppointmentStatus status,
        UUID createdBy,
        String cancelledReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatientId(),
                a.getDoctorId(),
                a.getStartAt(),
                a.getEndAt(),
                a.getType(),
                a.getReason(),
                a.getStatus(),
                a.getCreatedBy(),
                a.getCancelledReason(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
