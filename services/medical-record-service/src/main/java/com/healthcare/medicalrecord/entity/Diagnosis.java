package com.healthcare.medicalrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * ICD-10-coded diagnosis entry. Belongs to one {@link MedicalRecord}.
 */
@Entity
@Table(name = "diagnoses")
public class Diagnosis {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "icd10_code", nullable = false, updatable = false, length = 16)
    private String icd10Code;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Diagnosis() {
        // JPA
    }

    public static Diagnosis create(UUID recordId, String icd10Code, String description,
                                   LocalDate onsetDate) {
        Diagnosis d = new Diagnosis();
        d.id = UUID.randomUUID();
        d.recordId = Objects.requireNonNull(recordId, "recordId");
        d.icd10Code = Objects.requireNonNull(icd10Code, "icd10Code").trim().toUpperCase();
        d.description = Objects.requireNonNull(description, "description").trim();
        d.onsetDate = onsetDate;
        return d;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRecordId() { return recordId; }
    public String getIcd10Code() { return icd10Code; }
    public String getDescription() { return description; }
    public LocalDate getOnsetDate() { return onsetDate; }
    public Instant getCreatedAt() { return createdAt; }
}
