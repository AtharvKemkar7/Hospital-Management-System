package com.healthcare.doctor.security;

import java.util.UUID;

/**
 * Roles mirrored from Auth Service. Used for authority checks.
 * Single source of truth: the {@code role} claim of the JWT.
 */
public enum Role {
    PATIENT,
    DOCTOR,
    ADMIN,
    RECEPTIONIST,
    BILLING_STAFF;

    public String authority() {
        return "ROLE_" + name();
    }
}
