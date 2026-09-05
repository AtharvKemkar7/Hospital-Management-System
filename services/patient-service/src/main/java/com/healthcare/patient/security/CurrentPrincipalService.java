package com.healthcare.patient.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Returns the {@link PatientPrincipal} of the currently-authenticated
 * caller. Used by the controller layer to obtain {@code userId} and
 * {@code role} without touching the SecurityContext directly.
 */
@Component
public class CurrentPrincipalService {

    public PatientPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof PatientPrincipal p)) {
            throw new IllegalStateException("No authenticated principal in security context");
        }
        return p;
    }

    public UUID currentUserId() {
        return currentPrincipal().getUserId();
    }

    public Role currentRole() {
        return currentPrincipal().getRole();
    }
}
