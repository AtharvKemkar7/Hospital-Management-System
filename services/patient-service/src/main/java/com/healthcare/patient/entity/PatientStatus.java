package com.healthcare.patient.entity;

/**
 * Patient lifecycle states.
 *
 * <p>Phase 2 design (single-role, single-table):
 * <ul>
 *   <li>{@code PENDING}     - created via API by an authenticated PATIENT; awaiting initial verification (Phase 2: effectively a stub; verification flow not implemented in Phase 2).</li>
 *   <li>{@code ACTIVE}      - normal state.</li>
 *   <li>{@code INACTIVE}    - set by an admin or via the (deferred) {@code UserDeactivated} consumer.</li>
 *   <li>{@code DECEASED}    - terminal state set by an admin.</li>
 * </ul>
 */
public enum PatientStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
    DECEASED;

    public boolean canBeReadByOwner() {
        return this == ACTIVE || this == PENDING;
    }
}
