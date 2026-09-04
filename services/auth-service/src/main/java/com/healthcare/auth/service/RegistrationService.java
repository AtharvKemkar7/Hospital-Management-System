package com.healthcare.auth.service;

import com.healthcare.auth.config.AuthProperties;
import com.healthcare.auth.dto.request.CreateUserRequest;
import com.healthcare.auth.dto.request.RegisterRequest;
import com.healthcare.auth.entity.Role;
import com.healthcare.auth.entity.User;
import com.healthcare.auth.exception.DuplicateEmailException;
import com.healthcare.auth.exception.WeakPasswordException;
import com.healthcare.auth.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration logic. Used by both the public self-registration endpoint
 * and the admin-only user-creation endpoint.
 *
 * <p>Privilege-escalation guard: the public endpoint always creates a
 * {@code PATIENT} account. The {@link #createAsAdmin} method is only
 * reachable from the admin-only {@code POST /api/v1/users} endpoint,
 * which is itself guarded by Spring Security.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final UserEventPublisher events;
    private final AuthProperties props;
    private final PasswordPolicy passwordPolicy;

    public RegistrationService(UserRepository users,
                               PasswordEncoder encoder,
                               UserEventPublisher events,
                               AuthProperties props,
                               PasswordPolicy passwordPolicy) {
        this.users = users;
        this.encoder = encoder;
        this.events = events;
        this.props = props;
        this.passwordPolicy = passwordPolicy;
    }

    /** Public self-registration. Always creates a PATIENT account. */
    @Transactional
    public User registerPublic(RegisterRequest req, String correlationId) {
        Role role = parsePublicRole();
        return register(req.email(), req.password(), req.firstName(), req.lastName(),
                role, true /*active*/, correlationId);
    }

    /** Admin-only: create a user of any role. */
    @Transactional
    public User createAsAdmin(CreateUserRequest req, String correlationId) {
        // Admins can also be created through this path. The role is taken
        // directly from the request.
        return register(req.email(), req.password(), req.firstName(), req.lastName(),
                req.role(), true /*active*/, correlationId);
    }

    private User register(String email, String rawPassword,
                          String firstName, String lastName,
                          Role role, boolean activateImmediately,
                          String correlationId) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new DuplicateEmailException();
        }
        String policyFailure = passwordPolicy.validate(rawPassword);
        if (policyFailure != null) {
            throw new WeakPasswordException(policyFailure);
        }
        User user = User.newUser(
                normalized,
                encoder.encode(rawPassword),
                firstName.trim(),
                lastName.trim(),
                role);

        if (activateImmediately) {
            // In Phase 1 we do not run an email-verification flow, so newly
            // registered users are immediately ACTIVE. The status column is
            // kept so the email-verification step can be added later
            // without a schema change.
            user.activate();
        }

        try {
            users.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created the same email
            // between the existsByEmail check and the save.
            throw new DuplicateEmailException();
        }
        log.info("User registered: userId={}, role={}", user.getId(), role);
        events.publishUserRegistered(user, correlationId);
        return user;
    }

    private Role parsePublicRole() {
        String name = props.getRegistration().getPublicRole();
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            log.warn("Configured public role '{}' is not a valid Role; defaulting to PATIENT", name);
            return Role.PATIENT;
        }
    }
}
