package com.healthcare.prescription.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthcare.prescription.entity.Prescription;
import com.healthcare.prescription.entity.PrescriptionItem;
import com.healthcare.prescription.entity.PrescriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Safe projection of a prescription. Identifiers are minimal: no
 * email, no full name, no clinical payload beyond the documented
 * medication fields.
 */
public record PrescriptionResponse(
        UUID id,
        UUID patientId,
        UUID doctorId,
        UUID appointmentId,
        Instant issuedAt,
        PrescriptionStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<PrescriptionItemResponse> items
) {

    public static PrescriptionResponse from(Prescription p, List<PrescriptionItem> items) {
        return new PrescriptionResponse(
                p.getId(),
                p.getPatientId(),
                p.getDoctorId(),
                p.getAppointmentId(),
                p.getIssuedAt(),
                p.getStatus(),
                p.getNotes(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                items == null ? null : items.stream().map(PrescriptionItemResponse::from).toList()
        );
    }

    public static PrescriptionResponse summaryOnly(Prescription p) {
        return new PrescriptionResponse(
                p.getId(), p.getPatientId(), p.getDoctorId(), p.getAppointmentId(),
                p.getIssuedAt(), p.getStatus(), p.getNotes(),
                p.getCreatedAt(), p.getUpdatedAt(), null
        );
    }

    public record PrescriptionItemResponse(
            UUID id,
            String drugName,
            String dosage,
            String frequency,
            String route,
            Integer durationDays,
            Integer quantity,
            String instructions,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static PrescriptionItemResponse from(PrescriptionItem i) {
            return new PrescriptionItemResponse(
                    i.getId(), i.getDrugName(), i.getDosage(), i.getFrequency(),
                    i.getRoute(), i.getDurationDays(), i.getQuantity(),
                    i.getInstructions(), i.getCreatedAt(), i.getUpdatedAt()
            );
        }
    }
}
