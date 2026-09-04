package com.healthcare.auth.repository;

import com.healthcare.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes every active (non-revoked, non-expired) refresh token that
     * belongs to a user. Used on logout and on account disable.
     */
    @Modifying
    @Query("""
           update RefreshToken t
              set t.revokedAt = :now
            where t.userId = :userId
              and t.revokedAt is null
           """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
