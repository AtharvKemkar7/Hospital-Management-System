package com.healthcare.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh-token aggregate.
 *
 * <p>The raw token is a high-entropy random value (256 bits) generated
 * server-side and returned to the client exactly once at issuance. Only
 * the SHA-256 hash is persisted. The hash is the lookup key during refresh
 * and logout.
 *
 * <p>Rotation: when a refresh token is used, the previous row is marked
 * {@code revoked_at = now()} and {@code replaced_by = <new id>}. The new
 * row is created in the same transaction.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
        // JPA
    }

    public static RefreshToken issue(UUID userId, String tokenHash, Instant now,
                                     java.time.Duration ttl,
                                     String userAgent, String ip) {
        RefreshToken t = new RefreshToken();
        t.id = UUID.randomUUID();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.issuedAt = now;
        t.expiresAt = now.plus(ttl);
        t.userAgent = userAgent;
        t.ip = ip;
        return t;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(Instant now, UUID replacedBy) {
        if (this.revokedAt != null) {
            return; // idempotent
        }
        this.revokedAt = now;
        this.replacedBy = replacedBy;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getReplacedBy() { return replacedBy; }
    public String getUserAgent() { return userAgent; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }
}
