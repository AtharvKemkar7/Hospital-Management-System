package com.healthcare.patient.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT verification configuration. Bound from {@code app.jwt.*}.
 * Must match the {@code auth-service} secret and issuer (HS256).
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    @NotBlank
    private String secret;

    @NotBlank
    private String issuer = "healthcare-platform";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
