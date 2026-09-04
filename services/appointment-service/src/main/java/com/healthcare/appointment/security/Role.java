package com.healthcare.appointment.security;

import java.util.UUID;

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
