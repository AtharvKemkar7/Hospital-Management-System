package com.healthcare.auth.service;

import org.springframework.stereotype.Component;

/**
 * Password policy.
 *
 * <p>Initial policy (Phase 1):
 * <ul>
 *   <li>Length between 12 and 128 characters.</li>
 *   <li>At least one letter and at least one digit. (Other classes are
 *       not enforced so that long passphrases are accepted.)</li>
 *   <li>No whitespace-only values.</li>
 * </ul>
 *
 * <p>Policy is intentionally simple: the JWT and refresh-token entropy
 * already come from the server, so the password only needs to be
 * non-trivial. Returns {@code null} on success, or a human-readable
 * reason on failure.
 */
@Component
public class PasswordPolicy {

    public String validate(String raw) {
        if (raw == null) {
            return "password is required";
        }
        if (raw.length() < 12 || raw.length() > 128) {
            return "password must be between 12 and 128 characters";
        }
        if (raw.isBlank()) {
            return "password must not be blank";
        }
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            return "password must contain at least one letter and one digit";
        }
        return null;
    }
}
