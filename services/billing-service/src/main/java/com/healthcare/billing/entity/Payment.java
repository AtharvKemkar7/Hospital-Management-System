package com.healthcare.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment record. A payment moves an invoice toward PAID. No real
 * payment-provider integration exists in Phase 7 — this is the
 * internal state model only.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 16)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    @Column(name = "reference", length = 200)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
        // JPA
    }

    public static Payment create(UUID invoiceId, PaymentMethod method,
                                 BigDecimal amount, String currency,
                                 Instant paidAt, String reference) {
        Payment p = new Payment();
        p.id = UUID.randomUUID();
        p.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
        p.method = Objects.requireNonNull(method, "method");
        p.amount = Objects.requireNonNull(amount, "amount");
        p.currency = Objects.requireNonNull(currency, "currency");
        p.paidAt = paidAt == null ? Instant.now() : paidAt;
        p.reference = reference;
        return p;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoiceId; }
    public PaymentMethod getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getPaidAt() { return paidAt; }
    public String getReference() { return reference; }
    public Instant getCreatedAt() { return createdAt; }
}
