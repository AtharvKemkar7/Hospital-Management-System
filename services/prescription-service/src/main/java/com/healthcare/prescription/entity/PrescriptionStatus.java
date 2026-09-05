package com.healthcare.prescription.entity;

/**
 * Prescription lifecycle states.
 *
 * <p>Documented in {@code docs/service-boundaries.md} §7.
 *
 * <p>Allowed transitions (enforced in
 * {@code com.healthcare.prescription.service.PrescriptionService}):
 * <pre>
 *   ISSUED     -&gt; CANCELLED | DISPENSED
 *   CANCELLED  -&gt; (terminal)
 *   DISPENSED  -&gt; (terminal)
 * </pre>
 *
 * <p>Items can be added to a prescription only while it is in
 * {@code ISSUED}. Once {@code CANCELLED} or {@code DISPENSED}, the
 * prescription and its items are immutable.
 */
public enum PrescriptionStatus {
    ISSUED,
    CANCELLED,
    DISPENSED;

    public boolean isTerminal() {
        return this == CANCELLED || this == DISPENSED;
    }

    public boolean canAcceptItems() {
        return this == ISSUED;
    }
}
