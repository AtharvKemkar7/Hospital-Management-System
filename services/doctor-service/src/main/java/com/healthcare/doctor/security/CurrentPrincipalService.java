package com.healthcare.doctor.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentPrincipalService {

    public DoctorPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof DoctorPrincipal p)) {
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
