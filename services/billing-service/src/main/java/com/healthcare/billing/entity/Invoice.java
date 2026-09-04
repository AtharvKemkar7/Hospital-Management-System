package com.healthcare.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice aggregate. Owned exclusively by the Billing Service.
 *
 * <p>Cross-service references ({@code patientId}, {@code appointmentId})
 * are plain UUID columns; the corresponding rows live in other
 * services' databases. No JPA relationships to those entities are
 * defined here.
 *
 * <p>The {@code totalAmount} is computed at creation from the invoice
 * items and is <b>never</b> recomputed from the client. All monetary
 * values are {@link BigDecimal} at scale 4.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "appointment_id", updatable = false)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Invoice() {
        // JPA
    }

    /**
     * Create a new DRAFT invoice with a server-computed total.
     * The status is {@code DRAFT} until billing staff issues it.
     */
    public static Invoice create(UUID patientId, UUID appointmentId,
                                 BigDecimal totalAmount, String currency,
                                 Instant dueAt) {
        Invoice i = new Invoice();
        i.id = UUID.randomUUID();
        i.patientId = Objects.requireNonNull(patientId, "patientId");
        i.appointmentId = appointmentId;
        i.status = InvoiceStatus.DRAFT;
        i.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount");
        i.currency = Objects.requireNonNull(currency, "currency");
        i.dueAt = dueAt;
        return i;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (currency == null) currency = "USD";
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void issue() {
        this.status = InvoiceStatus.ISSUED;
        if (this.issuedAt == null) this.issuedAt = Instant.now();
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
    }

    public void voidInvoice() {
        this.status = InvoiceStatus.VOID;
    }

    public void markRefunded() {
        this.status = InvoiceStatus.REFUNDED;
    }

    // ---------- accessors ----------

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getAppointmentId() { return appointmentId; }
    public InvoiceStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
