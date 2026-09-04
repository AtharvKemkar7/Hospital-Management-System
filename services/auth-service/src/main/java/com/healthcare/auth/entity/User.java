package com.healthcare.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Platform user aggregate. Owned exclusively by the Auth Service.
 *
 * <p>Email is stored lowercased; uniqueness is enforced at the database
 * level ({@code uq_users_email}). Passwords are stored as BCrypt hashes;
 * the raw value is never persisted.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    /**
     * Creates a new user. The caller is responsible for providing a BCrypt
     * password hash, not the raw password.
     */
    public static User newUser(String email, String passwordHash,
                               String firstName, String lastName, Role role) {
        User u = new User();
        u.id = UUID.randomUUID();
        u.email = Objects.requireNonNull(email, "email").toLowerCase();
        u.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        u.firstName = Objects.requireNonNull(firstName, "firstName");
        u.lastName = Objects.requireNonNull(lastName, "lastName");
        u.role = Objects.requireNonNull(role, "role");
        u.status = AccountStatus.PENDING_VERIFICATION;
        u.emailVerified = false;
        u.failedLoginCount = 0;
        return u;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ---------- behavior ----------

    public void activate() {
        this.status = AccountStatus.ACTIVE;
        this.emailVerified = true;
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    public void recordSuccessfulLogin(Instant now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Increments the failed-login counter. If the new count meets or exceeds
     * the threshold, locks the account for the given duration.
     */
    public void recordFailedLogin(int lockoutThreshold, java.time.Duration lockoutDuration, Instant now) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= lockoutThreshold) {
            this.status = AccountStatus.LOCKED;
            this.lockedUntil = now.plus(lockoutDuration);
        }
    }

    public boolean isLockedAt(Instant now) {
        if (status == AccountStatus.LOCKED && lockedUntil != null && now.isBefore(lockedUntil)) {
            return true;
        }
        return false;
    }

    public void disable() {
        this.status = AccountStatus.DISABLED;
    }

    public void deactivate() {
        this.status = AccountStatus.DEACTIVATED;
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "newRole");
    }

    public void changePasswordHash(String newHash) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash");
    }

    // ---------- accessors ----------

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public boolean isEmailVerified() { return emailVerified; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
