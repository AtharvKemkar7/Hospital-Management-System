package com.healthcare.medicalrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Medical record aggregate. Owned exclusively by the Medical Record
 * Service.
 *
 * <p>Cross-service references ({@code patientId}, {@code doctorId},
 * {@code appointmentId}) are plain UUID columns; the corresponding
 * rows live in other services' databases.
 *
 * <p>Records are append-only by design: there is no PATCH endpoint.
 * The record is created once and is extended through {@code diagnoses}
 * and {@code vitals} sub-records.
 */
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false, updatable = false)
    private UUID doctorId;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private UUID appointmentId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "summary", length = 4000)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicalRecord() {
        // JPA
    }

    public static MedicalRecord create(UUID patientId, UUID doctorId,
                                       UUID appointmentId, String summary) {
        MedicalRecord r = new MedicalRecord();
        r.id = UUID.randomUUID();
        r.patientId = Objects.requireNonNull(patientId, "patientId");
        r.doctorId = Objects.requireNonNull(doctorId, "doctorId");
        r.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        r.summary = summary;
        return r;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (recordedAt == null) recordedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getDoctorId() { return doctorId; }
    public UUID getAppointmentId() { return appointmentId; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
