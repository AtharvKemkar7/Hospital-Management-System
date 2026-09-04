package com.healthcare.appointment.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
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
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenVerifier.class);
    private static final int MIN_SECRET_BYTES_PROD = 32;

    private final JwtProperties props;
    private final Clock clock;
    private final Environment env;
    private JWSVerifier verifier;

    public JwtTokenVerifier(JwtProperties props, Clock clock, Environment env) {
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
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (isProductionProfile() && bytes.length < MIN_SECRET_BYTES_PROD) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES_PROD
                            + " bytes in production. Got " + bytes.length + ".");
        }
        try {
            this.verifier = new MACVerifier(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize JWT verifier", e);
        }
        log.info("JWT verifier initialized (HS256, {} bytes)", bytes.length);
    }

    private boolean isProductionProfile() {
        for (String p : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return Arrays.asList(env.getDefaultProfiles()).contains("prod");
    }

    public ParsedToken parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
                throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
            }
            if (!jwt.verify(verifier)) {
                throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(clock.instant())) {
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
            return new ParsedToken(UUID.fromString(userId), Role.valueOf(roleName));
        } catch (ParseException | IllegalArgumentException e) {
            throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new InvalidJwtException(InvalidJwtException.Reason.INVALID);
        }
    }

    public record ParsedToken(UUID userId, Role role) { }

    public static class InvalidJwtException extends RuntimeException {
        public enum Reason { INVALID, EXPIRED }
        private final Reason reason;
        public InvalidJwtException(Reason r) { super(r.name()); this.reason = r; }
        public Reason getReason() { return reason; }
    }
}
