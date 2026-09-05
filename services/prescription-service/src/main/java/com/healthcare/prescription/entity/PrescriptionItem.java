package com.healthcare.prescription.entity;

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
 * A medication item belonging to a {@link Prescription}.
 *
 * <p>Fields mirror {@code docs/database-design.md} §8.6:
 * {@code drug_name, dosage, frequency, route, duration_days,
 * instructions}, with the addition of {@code quantity} and the
 * standard audit timestamps for consistency with the rest of the
 * platform.
 */
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prescription_id", nullable = false, updatable = false)
    private UUID prescriptionId;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 100)
    private String frequency;

    @Column(name = "route", length = 50)
    private String route;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "instructions", length = 1000)
    private String instructions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PrescriptionItem() {
        // JPA
    }

    public static PrescriptionItem create(UUID prescriptionId, String drugName,
                                          String dosage, String frequency,
                                          String route, Integer durationDays,
                                          Integer quantity, String instructions) {
        PrescriptionItem i = new PrescriptionItem();
        i.id = UUID.randomUUID();
        i.prescriptionId = Objects.requireNonNull(prescriptionId, "prescriptionId");
        i.drugName = Objects.requireNonNull(drugName, "drugName").trim();
        i.dosage = Objects.requireNonNull(dosage, "dosage").trim();
        i.frequency = Objects.requireNonNull(frequency, "frequency").trim();
        i.route = route == null ? null : route.trim();
        i.durationDays = durationDays;
        i.quantity = quantity;
        i.instructions = instructions;
        return i;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPrescriptionId() { return prescriptionId; }
    public String getDrugName() { return drugName; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public String getRoute() { return route; }
    public Integer getDurationDays() { return durationDays; }
    public Integer getQuantity() { return quantity; }
    public String getInstructions() { return instructions; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
