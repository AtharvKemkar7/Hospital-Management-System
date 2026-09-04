package com.healthcare.billing.entity;

/**
 * Invoice lifecycle states.
 *
 * <p>Documented in {@code docs/service-boundaries.md} §8 and
 * {@code docs/database-design.md} §8.7.
 *
 * <p>Allowed transitions (enforced in
 * {@code com.healthcare.billing.service.InvoiceService}):
 * <pre>
 *   DRAFT      -&gt; ISSUED | VOID
 *   ISSUED     -&gt; PAID | VOID
 *   PAID       -&gt; REFUNDED
 *   VOID       -&gt; (terminal)
 *   REFUNDED   -&gt; (terminal)
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    VOID,
    REFUNDED;

    public boolean isTerminal() {
        return this == VOID || this == REFUNDED;
    }
}
