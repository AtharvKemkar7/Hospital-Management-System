package com.healthcare.doctor.entity;

/**
 * Doctor professional lifecycle states.
 *
 * <p>Phase 3 design (single-table):
 * <ul>
 *   <li>{@code ACTIVE}    - normal state. Visible to patients when listed.</li>
 *   <li>{@code ON_LEAVE}  - doctor is away (e.g., vacation). May be excluded from patient-facing listings in a future phase.</li>
 *   <li>{@code INACTIVE}  - set by an admin or via the (deferred) {@code UserDeactivated} consumer.</li>
 * </ul>
 */
public enum DoctorStatus {
    ACTIVE,
    INACTIVE,
    ON_LEAVE
}
