package com.healthcare.appointment.entity;

/**
 * Appointment lifecycle states.
 *
 * <p>Documented in {@code docs/service-boundaries.md} §5 and
 * {@code docs/database-design.md} §8.4.
 *
 * <p>Allowed transitions (enforced in
 * {@code com.healthcare.appointment.service.AppointmentService}):
 * <pre>
 *   REQUESTED  -&gt; CONFIRMED | CANCELLED
 *   CONFIRMED  -&gt; CANCELLED | COMPLETED | NO_SHOW
 *   CANCELLED  -&gt; (terminal)
 *   COMPLETED  -&gt; (terminal)
 *   NO_SHOW    -&gt; (terminal)
 * </pre>
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED || this == NO_SHOW;
    }
}
