package com.healthcare.auth.entity;

/**
 * Platform roles. The string form matches the JWT {@code role} claim and
 * the {@code user_roles} database column.
 *
 * <p>New roles can be added by appending a constant and updating the database
 * {@code ck_users_role} check constraint via a Flyway migration. Existing
 * services that read the claim should treat unknown values as deny-by-default.
 *
 * <p><b>Spring Security authority naming:</b> when granted to an
 * authenticated principal, the role is exposed as {@code "ROLE_<NAME>"} (e.g.
 * {@code ROLE_PATIENT}). This follows the Spring convention so that
 * {@code @PreAuthorize("hasRole('ADMIN')")} and
 * {@code @PreAuthorize("hasAuthority('ROLE_PATIENT')")} both work.
 */
public enum Role {
    PATIENT,
    DOCTOR,
    ADMIN,
    RECEPTIONIST,
    BILLING_STAFF;

    /** Spring Security authority for this role, e.g. {@code ROLE_PATIENT}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
