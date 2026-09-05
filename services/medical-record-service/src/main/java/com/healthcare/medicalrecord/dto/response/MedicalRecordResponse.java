package com.healthcare.medicalrecord.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthcare.medicalrecord.entity.Diagnosis;
import com.healthcare.medicalrecord.entity.MedicalRecord;
import com.healthcare.medicalrecord.entity.Vital;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Safe projection of a medical record. Includes the list of diagnoses
 * and vitals that belong to it. Identifiers are intentionally minimal:
 * no email, no full name — clients cross-reference to Patient and
 * Doctor services if they need those.
 */
public record MedicalRecordResponse(
        UUID id,
        UUID patientId,
        UUID doctorId,
        UUID appointmentId,
        Instant recordedAt,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<DiagnosisResponse> diagnoses,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<VitalResponse> vitals
) {

    public static MedicalRecordResponse from(MedicalRecord r,
                                              List<Diagnosis> diagnoses,
                                              List<Vital> vitals) {
        return new MedicalRecordResponse(
                r.getId(),
                r.getPatientId(),
                r.getDoctorId(),
                r.getAppointmentId(),
                r.getRecordedAt(),
                r.getSummary(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                diagnoses == null ? null : diagnoses.stream().map(DiagnosisResponse::from).toList(),
                vitals == null ? null : vitals.stream().map(VitalResponse::from).toList()
        );
    }

    public static MedicalRecordResponse summaryOnly(MedicalRecord r) {
        return new MedicalRecordResponse(
                r.getId(), r.getPatientId(), r.getDoctorId(), r.getAppointmentId(),
                r.getRecordedAt(), r.getSummary(),
                r.getCreatedAt(), r.getUpdatedAt(),
                null, null
        );
    }

    public record DiagnosisResponse(
            UUID id,
            String icd10Code,
            String description,
            LocalDate onsetDate,
            Instant createdAt
    ) {
        public static DiagnosisResponse from(Diagnosis d) {
            return new DiagnosisResponse(
                    d.getId(), d.getIcd10Code(), d.getDescription(),
                    d.getOnsetDate(), d.getCreatedAt()
            );
        }
    }

    public record VitalResponse(
            UUID id,
            Instant takenAt,
            Integer systolic,
            Integer diastolic,
            Integer heartRate,
            BigDecimal temperatureC,
            Integer spo2,
            Instant createdAt
    ) {
        public static VitalResponse from(Vital v) {
            return new VitalResponse(
                    v.getId(), v.getTakenAt(),
                    v.getSystolic(), v.getDiastolic(),
                    v.getHeartRate(), v.getTemperatureC(), v.getSpo2(),
                    v.getCreatedAt()
            );
        }
    }
}
