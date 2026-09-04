package com.healthcare.auth.service;

import com.healthcare.auth.config.AuthProperties;
import com.healthcare.auth.config.JwtProperties;
import com.healthcare.auth.entity.AccountStatus;
import com.healthcare.auth.entity.RefreshToken;
import com.healthcare.auth.entity.User;
import com.healthcare.auth.exception.AccountLockedException;
import com.healthcare.auth.exception.AccountNotActiveException;
import com.healthcare.auth.exception.AuthenticationFailedException;
import com.healthcare.auth.exception.InvalidTokenException;
import com.healthcare.auth.repository.RefreshTokenRepository;
import com.healthcare.auth.repository.UserRepository;
import com.healthcare.auth.security.JwtTokenProvider;
import com.healthcare.auth.util.HashUtil;
import com.healthcare.auth.util.SecureTokenGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Login, logout, and refresh-token operations.
 *
 * <p>User-enumeration guard: every failure path returns the same generic
 * 401 with the same message. No difference is observable in the API
 * response or in logs beyond a single high-level line.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    /** A throwaway BCrypt hash used to equalize response time on "user not found". */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$CwTycUXWue0Thq9StjUM0uJ8oC1JmBGeJqJ5Z9q5Z9q5Z9q5Z9q5Zu";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;
    private final AuthProperties props;
    private final Clock clock;

    public LoginService(UserRepository users,
                        RefreshTokenRepository refreshTokens,
                        PasswordEncoder encoder,
                        JwtTokenProvider jwt,
                        JwtProperties jwtProps,
                        AuthProperties props,
                        Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
        this.props = props;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- LOGIN

    @Transactional
    public LoginResult login(String email, String rawPassword,
                             String userAgent, String ip) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        User user = users.findByEmail(normalized).orElse(null);

        if (user == null) {
            encoder.matches(rawPassword == null ? "" : rawPassword, DUMMY_BCRYPT_HASH);
            log.info("Authentication failed (no such user) for email={}", normalized);
            throw new AuthenticationFailedException();
        }

        Instant now = clock.instant();
        if (user.isLockedAt(now)) {
            log.info("Authentication failed (locked) for userId={}", user.getId());
            throw new AccountLockedException();
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.info("Authentication failed (not active) for userId={}", user.getId());
            throw new AccountNotActiveException();
        }
        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            int threshold = props.getSecurity().getMaxFailedLoginAttempts();
            Duration lockout = props.getSecurity().getLockoutDuration();
            user.recordFailedLogin(threshold, lockout, now);
            users.save(user);
            log.info("Authentication failed (bad credentials) for userId={}", user.getId());
            throw new AuthenticationFailedException();
        }

        user.recordSuccessfulLogin(now);
        users.save(user);
        log.info("Authentication successful for userId={}", user.getId());

        JwtTokenProvider.IssuedToken access = jwt.issueAccessToken(user);
        IssuedRefresh refresh = issueRefreshToken(user, userAgent, ip);
        return new LoginResult(user, access, refresh);
    }

    // --------------------------------------------------------------- REFRESH

    @Transactional
    public RefreshResult refresh(String presentedToken, String userAgent, String ip) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw InvalidTokenException.invalid();
        }
        String hash = HashUtil.sha256(presentedToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(InvalidTokenException::invalid);

        Instant now = clock.instant();
        if (stored.getRevokedAt() != null) {
            // Reuse of a revoked token is a strong signal of compromise.
            refreshTokens.revokeAllForUser(stored.getUserId(), now);
            log.warn("Refresh-token replay detected; all tokens revoked for userId={}",
                    stored.getUserId());
            throw InvalidTokenException.revoked();
        }
        if (now.isAfter(stored.getExpiresAt())) {
            throw InvalidTokenException.expired();
        }

        User user = users.findById(stored.getUserId())
                .orElseThrow(InvalidTokenException::invalid);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            stored.revoke(now, null);
            refreshTokens.save(stored);
            throw InvalidTokenException.invalid();
        }

        IssuedRefresh newRefresh = issueRefreshToken(user, userAgent, ip);
        stored.revoke(now, newRefresh.token().getId());
        refreshTokens.save(stored);

        JwtTokenProvider.IssuedToken access = jwt.issueAccessToken(user);
        return new RefreshResult(user, access, newRefresh);
    }

    // ---------------------------------------------------------------- LOGOUT

    @Transactional
    public void logout(UUID userId) {
        Instant now = clock.instant();
        int n = refreshTokens.revokeAllForUser(userId, now);
        log.info("Logout: revoked {} refresh token(s) for userId={}", n, userId);
    }

    // ----------------------------------------------------------------- core

    private IssuedRefresh issueRefreshToken(User user, String userAgent, String ip) {
        String raw = SecureTokenGenerator.generate();
        String hash = HashUtil.sha256(raw);
        Instant now = clock.instant();
        Duration ttl = Duration.ofSeconds(jwtProps.getRefreshTokenExpirationSeconds());
        RefreshToken t = RefreshToken.issue(user.getId(), hash, now, ttl, userAgent, ip);
        refreshTokens.save(t);
        return new IssuedRefresh(t, raw);
    }

    // ----------------------------------------------------------------- types

    /** Persisted entity paired with the raw value (returned to caller exactly once). */
    public record IssuedRefresh(RefreshToken token, String raw) { }

    public record LoginResult(User user, JwtTokenProvider.IssuedToken access, IssuedRefresh refresh) { }
    public record RefreshResult(User user, JwtTokenProvider.IssuedToken access, IssuedRefresh refresh) { }
}
