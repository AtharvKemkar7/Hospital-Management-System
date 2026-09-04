package com.healthcare.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration. Bound from {@code app.jwt.*}.
 *
 * <p>The signing secret is HS256 (HMAC-SHA-256). 32 bytes / 256 bits is
 * the recommended minimum. In the {@code prod} profile the secret MUST be
 * provided via the {@code JWT_SECRET} environment variable and the
 * minimum-length rule is enforced in {@link com.healthcare.auth.security.JwtTokenProvider}.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    /** HMAC-SHA-256 secret. Must be at least 32 bytes in production. */
    @NotBlank
    private String secret;

    /** Access-token lifetime in seconds. */
    @Min(60)
    private long accessTokenExpirationSeconds = 900;

    /** Refresh-token lifetime in seconds. */
    @Min(60)
    private long refreshTokenExpirationSeconds = 1_209_600; // 14 days

    @NotBlank
    private String issuer = "healthcare-platform";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenExpirationSeconds() { return accessTokenExpirationSeconds; }
    public void setAccessTokenExpirationSeconds(long v) { this.accessTokenExpirationSeconds = v; }

    public long getRefreshTokenExpirationSeconds() { return refreshTokenExpirationSeconds; }
    public void setRefreshTokenExpirationSeconds(long v) { this.refreshTokenExpirationSeconds = v; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
