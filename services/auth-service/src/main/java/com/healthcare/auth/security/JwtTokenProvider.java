package com.healthcare.auth.security;

import com.healthcare.auth.config.JwtProperties;
import com.healthcare.auth.entity.Role;
import com.healthcare.auth.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256 JWTs.
 *
 * <p>Claims (minimum necessary):
 * <ul>
 *   <li>{@code sub}    -- user UUID (the principal identifier)</li>
 *   <li>{@code userId} -- same value, explicit for downstream readability</li>
 *   <li>{@code role}   -- single role name, e.g. {@code PATIENT}</li>
 *   <li>{@code iss}    -- configured issuer</li>
 *   <li>{@code iat}    -- issued at</li>
 *   <li>{@code exp}    -- expires at</li>
 *   <li>{@code jti}    -- random unique id (per-token)</li>
 * </ul>
 *
 * <p>No PHI, no email, no name. No refresh-token value.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_SECRET_BYTES_PROD = 32;

    private final JwtProperties props;
    private final Clock clock;
    private final Environment env;

    private byte[] secretBytes;
    private JWSSigner signer;
    private JWSVerifier verifier;

    public JwtTokenProvider(JwtProperties props, Clock clock, Environment env) {
        this.props = props;
        this.clock = clock;
        this.env = env;
    }

    @PostConstruct
    void init() {
        String secret = props.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (isProductionProfile() && secretBytes.length < MIN_SECRET_BYTES_PROD) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES_PROD
                            + " bytes in production. Got " + secretBytes.length + ".");
        }
        try {
            this.signer = new MACSigner(secretBytes);
            this.verifier = new MACVerifier(secretBytes);
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to initialize JWT signer/verifier", e);
        }
        log.info("JWT signer initialized (HS256, {} bytes)", secretBytes.length);
    }

    private boolean isProductionProfile() {
        for (String p : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return Arrays.asList(env.getDefaultProfiles()).contains("prod");
    }

    /** Issue an access token for the given user. */
    public IssuedToken issueAccessToken(User user) {
        Instant now = clock.instant();
        Instant exp = now.plusSeconds(props.getAccessTokenExpirationSeconds());
        String token = sign(user.getId(), user.getRole(), now, exp);
        return new IssuedToken(token, exp);
    }

    private String sign(UUID userId, Role role, Instant iat, Instant exp) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issuer(props.getIssuer())
                    .issueTime(Date.from(iat))
                    .expirationTime(Date.from(exp))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("userId", userId.toString())
                    .claim("role", role.name())
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /** Parse and verify a JWT, returning its claims. */
    public ParsedToken parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null
                    || claims.getExpirationTime().toInstant().isBefore(clock.instant())) {
                throw new InvalidJwtException(InvalidJwtException.Reason.EXPIRED);
            }
            if (props.getIssuer() != null && !props.getIssuer().equals(claims.getIssuer())) {
                throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
            }
            String userId = claims.getStringClaim("userId");
            String roleName = claims.getStringClaim("role");
            if (userId == null || roleName == null) {
                throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
            }
            return new ParsedToken(UUID.fromString(userId), Role.valueOf(roleName),
                    claims.getExpirationTime().toInstant());
        } catch (ParseException | JOSEException e) {
            throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return props.getAccessTokenExpirationSeconds();
    }

    public record IssuedToken(String token, Instant expiresAt) { }
    public record ParsedToken(UUID userId, Role role, Instant expiresAt) { }

    public static class InvalidJwtException extends RuntimeException {
        public enum Reason { INVALID, EXPIRED }
        private final Reason reason;
        public InvalidJwtException(Reason r) { super(r.name()); this.reason = r; }
        public Reason getReason() { return reason; }
    }
}
