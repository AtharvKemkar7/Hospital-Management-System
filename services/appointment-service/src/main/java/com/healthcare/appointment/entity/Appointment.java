package com.healthcare.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Appointment aggregate. Owned exclusively by the Appointment Service.
 *
 * <p>Cross-service references ({@code patientId}, {@code doctorId},
 * {@code createdBy}) are plain UUID columns; the corresponding rows
 * live in other services' databases. No JPA relationships to those
 * entities are defined here.
 */
@Entity
@Table(name = "appointments",
        uniqueConstraints = {
                // Mirrors the partial unique index in V1__create_appointments.sql
                // for the production profile. The SQL migration uses a
                // partial index restricted to REQUESTED/CONCELLED rows;
                // the entity-level constraint applies to all rows and
                // is used by the H2 test profile where partial indexes
                // are not natively supported.
                @UniqueConstraint(name = "uq_appointments_doctor_slot",
                                  columnNames = {"doctor_id", "start_at"})
        })
public class Appointment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false, updatable = false)
    private UUID doctorId;

    @Column(name = "start_at", nullable = false, updatable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false, updatable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private AppointmentType type;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AppointmentStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
        // JPA
    }

    public static Appointment create(UUID patientId, UUID doctorId,
                                     Instant startAt, Instant endAt,
                                     AppointmentType type, String reason,
                                     UUID createdBy) {
        Appointment a = new Appointment();
        a.id = UUID.randomUUID();
        a.patientId = Objects.requireNonNull(patientId, "patientId");
        a.doctorId = Objects.requireNonNull(doctorId, "doctorId");
        a.startAt = Objects.requireNonNull(startAt, "startAt");
        a.endAt = Objects.requireNonNull(endAt, "endAt");
        a.type = type == null ? AppointmentType.IN_PERSON : type;
        a.reason = reason;
        a.status = AppointmentStatus.REQUESTED;
        a.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        return a;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = AppointmentStatus.REQUESTED;
        if (type == null) type = AppointmentType.IN_PERSON;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void confirm() {
        this.status = AppointmentStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledReason = reason;
    }

    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    public void markNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
    }

    // ---------- accessors ----------

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getDoctorId() { return doctorId; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public AppointmentType getType() { return type; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public String getCancelledReason() { return cancelledReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
