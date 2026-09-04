package com.healthcare.auth.entity;

/**
 * Account lifecycle states.
 *
 * <p>State machine (Phase 1):
 * <pre>
 *   PENDING_VERIFICATION ──► ACTIVE
 *           │
 *           └──► DISABLED       (admin action)
 *
 *   ACTIVE    ──► LOCKED         (too many failed logins; auto-unlocks)
 *   LOCKED    ──► ACTIVE         (after lockout duration, on next login)
 *   ACTIVE    ──► DEACTIVATED    (admin action; permanent)
 *   ACTIVE    ──► DISABLED       (admin action; reversible)
 * </pre>
 *
 * <p>Only {@code ACTIVE} users can authenticate. {@code LOCKED} and
 * {@code DISABLED} and {@code DEACTIVATED} and {@code PENDING_VERIFICATION}
 * are all rejected at login time — without revealing which one applies.
 */
public enum AccountStatus {
    /** Newly registered; not yet email-verified. Cannot log in. */
    PENDING_VERIFICATION,
    /** Normal authenticated state. */
    ACTIVE,
    /** Temporarily locked due to too many failed login attempts. */
    LOCKED,
    /** Disabled by an administrator. Reversible. */
    DISABLED,
    /** Deactivated (soft-deleted) by an administrator. Permanent. */
    DEACTIVATED;

    public boolean canAuthenticate() {
        return this == ACTIVE;
    }
}
