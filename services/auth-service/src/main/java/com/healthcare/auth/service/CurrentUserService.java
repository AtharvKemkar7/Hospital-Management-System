package com.healthcare.auth.service;

import com.healthcare.auth.entity.User;
import com.healthcare.auth.exception.ResourceNotFoundException;
import com.healthcare.auth.repository.UserRepository;
import com.healthcare.auth.security.AppPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Returns the user record corresponding to the currently-authenticated
 * principal. Used by /me and any service-layer code that needs the
 * user behind a JWT.
 */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        AppPrincipal p = currentPrincipal();
        return users.findById(p.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** Returns just the user id, without hitting the database. */
    public UUID currentUserId() {
        return currentPrincipal().getId();
    }

    private AppPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppPrincipal p)) {
            throw new ResourceNotFoundException("No authenticated principal");
        }
        return p;
    }
}
