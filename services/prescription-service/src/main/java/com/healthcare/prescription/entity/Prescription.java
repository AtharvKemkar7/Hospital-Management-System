package com.healthcare.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Prescription aggregate. Owned exclusively by the Prescription Service.
 *
 * <p>Cross-service references ({@code patientId}, {@code doctorId},
 * {@code appointmentId}) are plain UUID columns; the corresponding
 * rows live in other services' databases. No JPA relationships to
 * those entities are defined here.
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false, updatable = false)
    private UUID doctorId;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private UUID appointmentId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PrescriptionStatus status;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Prescription() {
        // JPA
    }

    public static Prescription create(UUID patientId, UUID doctorId,
                                      UUID appointmentId, String notes) {
        Prescription p = new Prescription();
        p.id = UUID.randomUUID();
        p.patientId = Objects.requireNonNull(patientId, "patientId");
        p.doctorId = Objects.requireNonNull(doctorId, "doctorId");
        p.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        p.notes = notes;
        p.status = PrescriptionStatus.ISSUED;
        return p;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (issuedAt == null) issuedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = PrescriptionStatus.ISSUED;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = PrescriptionStatus.CANCELLED;
    }

    public void dispense() {
        this.status = PrescriptionStatus.DISPENSED;
    }

    // ---------- accessors ----------

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getDoctorId() { return doctorId; }
    public UUID getAppointmentId() { return appointmentId; }
    public Instant getIssuedAt() { return issuedAt; }
    public PrescriptionStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
