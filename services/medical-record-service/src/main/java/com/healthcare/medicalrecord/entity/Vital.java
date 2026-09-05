package com.healthcare.medicalrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Vital-signs reading. Belongs to one {@link MedicalRecord}.
 *
 * <p>All numeric fields are nullable: a record can capture only the
 * values that were actually measured.
 */
@Entity
@Table(name = "vitals")
public class Vital {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "taken_at", nullable = false, updatable = false)
    private Instant takenAt;

    @Column(name = "systolic")
    private Integer systolic;

    @Column(name = "diastolic")
    private Integer diastolic;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "temperature_c", precision = 4, scale = 1)
    private BigDecimal temperatureC;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Vital() {
        // JPA
    }

    public static Vital create(UUID recordId, Instant takenAt,
                               Integer systolic, Integer diastolic,
                               Integer heartRate, BigDecimal temperatureC,
                               Integer spo2) {
        Vital v = new Vital();
        v.id = UUID.randomUUID();
        v.recordId = Objects.requireNonNull(recordId, "recordId");
        v.takenAt = takenAt == null ? Instant.now() : takenAt;
        v.systolic = systolic;
        v.diastolic = diastolic;
        v.heartRate = heartRate;
        v.temperatureC = temperatureC;
        v.spo2 = spo2;
        return v;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRecordId() { return recordId; }
    public Instant getTakenAt() { return takenAt; }
    public Integer getSystolic() { return systolic; }
    public Integer getDiastolic() { return diastolic; }
    public Integer getHeartRate() { return heartRate; }
    public BigDecimal getTemperatureC() { return temperatureC; }
    public Integer getSpo2() { return spo2; }
    public Instant getCreatedAt() { return createdAt; }
}
