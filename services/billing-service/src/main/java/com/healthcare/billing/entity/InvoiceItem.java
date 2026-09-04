package com.healthcare.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice line item. The {@code lineTotal} is computed server-side as
 * {@code quantity * unitPrice} (rounded HALF_UP at scale 4) and is
 * never trusted from the client.
 */
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16, updatable = false)
    private InvoiceItemSource source;

    @Column(name = "source_id", updatable = false)
    private UUID sourceId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvoiceItem() {
        // JPA
    }

    public static InvoiceItem create(UUID invoiceId, InvoiceItemSource source,
                                     UUID sourceId, String description,
                                     int quantity, BigDecimal unitPrice,
                                     BigDecimal lineTotal) {
        InvoiceItem i = new InvoiceItem();
        i.id = UUID.randomUUID();
        // invoiceId is bound separately via bindInvoiceId(...) once
        // the parent invoice has been persisted. The DB column is
        // NOT NULL, but the entity can exist briefly with a null
        // invoiceId during the in-memory construction phase.
        i.invoiceId = invoiceId;
        i.source = Objects.requireNonNull(source, "source");
        i.sourceId = sourceId;
        i.description = Objects.requireNonNull(description, "description").trim();
        i.quantity = quantity;
        i.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        i.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal");
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

    /**
     * Bind this item to its parent invoice. Intended to be called
     * once, immediately after the parent invoice has been persisted
     * and has received its id. Not exposed in any DTO or external
     * API.
     */
    public void bindInvoiceId(UUID invoiceId) {
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
    }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoiceId; }
    public InvoiceItemSource getSource() { return source; }
    public UUID getSourceId() { return sourceId; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
