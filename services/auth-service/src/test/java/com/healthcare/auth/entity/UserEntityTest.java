package com.healthcare.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void newUser_normalizesEmailToLowercase() {
        User u = User.newUser("User@Example.COM", "hash", "First", "Last", Role.PATIENT);
        assertThat(u.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void newUser_startsPendingAndUnverified() {
        User u = User.newUser("u@e.com", "hash", "F", "L", Role.PATIENT);
        assertThat(u.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(u.isEmailVerified()).isFalse();
        assertThat(u.getFailedLoginCount()).isZero();
        assertThat(u.getLockedUntil()).isNull();
    }

    @Test
    void recordFailedLogin_locksAfterThreshold() {
        User u = User.newUser("u@e.com", "hash", "F", "L", Role.PATIENT);
        u.activate();
        Instant t = Instant.parse("2026-01-01T10:00:00Z");

        for (int i = 0; i < 4; i++) {
            u.recordFailedLogin(5, Duration.ofMinutes(15), t);
        }
        assertThat(u.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(u.getFailedLoginCount()).isEqualTo(4);

        u.recordFailedLogin(5, Duration.ofMinutes(15), t);
        assertThat(u.getStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(u.getLockedUntil()).isEqualTo(t.plus(Duration.ofMinutes(15)));
    }

    @Test
    void isLockedAt_respectsLockedUntil() {
        User u = User.newUser("u@e.com", "hash", "F", "L", Role.PATIENT);
        u.activate();
        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        for (int i = 0; i < 5; i++) { u.recordFailedLogin(5, Duration.ofMinutes(15), t); }
        assertThat(u.getStatus()).isEqualTo(AccountStatus.LOCKED);

        assertThat(u.isLockedAt(t.plusSeconds(1))).isTrue();
        assertThat(u.isLockedAt(t.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    void recordSuccessfulLogin_resetsCounter() {
        User u = User.newUser("u@e.com", "hash", "F", "L", Role.PATIENT);
        u.activate();
        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        u.recordFailedLogin(5, Duration.ofMinutes(15), t);
        u.recordFailedLogin(5, Duration.ofMinutes(15), t);
        u.recordSuccessfulLogin(t);
        assertThat(u.getFailedLoginCount()).isZero();
        assertThat(u.getLastLoginAt()).isEqualTo(t);
    }

    @Test
    void changeRole_works() {
        User u = User.newUser("u@e.com", "hash", "F", "L", Role.PATIENT);
        u.changeRole(Role.DOCTOR);
        assertThat(u.getRole()).isEqualTo(Role.DOCTOR);
    }
}
