package com.healthcare.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Aggregates all non-JWT Auth Service configuration.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public class AuthProperties {

    private final Security security = new Security();
    private final Registration registration = new Registration();
    private final Events events = new Events();
    private final Cors cors = new Cors();
    private final Correlation correlation = new Correlation();

    public Security getSecurity() { return security; }
    public Registration getRegistration() { return registration; }
    public Events getEvents() { return events; }
    public Cors getCors() { return cors; }
    public Correlation getCorrelation() { return correlation; }

    public static class Security {
        /** BCrypt cost. 12 is the foundation default. */
        @Min(4)
        private int bcryptCost = 12;
        /** Failed-login threshold before lockout. */
        @Min(1)
        private int maxFailedLoginAttempts = 5;
        /** How long the account stays locked. */
        @NotNull
        private Duration lockoutDuration = Duration.ofMinutes(15);

        public int getBcryptCost() { return bcryptCost; }
        public void setBcryptCost(int v) { this.bcryptCost = v; }
        public int getMaxFailedLoginAttempts() { return maxFailedLoginAttempts; }
        public void setMaxFailedLoginAttempts(int v) { this.maxFailedLoginAttempts = v; }
        public Duration getLockoutDuration() { return lockoutDuration; }
        public void setLockoutDuration(Duration v) { this.lockoutDuration = v; }
    }

    public static class Registration {
        /**
         * The single role granted by public self-registration. Privileged
         * roles must be created through the admin endpoint.
         */
        private String publicRole = "PATIENT";

        public String getPublicRole() { return publicRole; }
        public void setPublicRole(String v) { this.publicRole = v; }
    }

    public static class Events {
        private boolean publishEnabled = false;
        private String topicUserRegistered = "healthcare.auth.UserRegistered";
        private String topicUserDeactivated = "healthcare.auth.UserDeactivated";

        public boolean isPublishEnabled() { return publishEnabled; }
        public void setPublishEnabled(boolean v) { this.publishEnabled = v; }
        public String getTopicUserRegistered() { return topicUserRegistered; }
        public void setTopicUserRegistered(String v) { this.topicUserRegistered = v; }
        public String getTopicUserDeactivated() { return topicUserDeactivated; }
        public void setTopicUserDeactivated(String v) { this.topicUserDeactivated = v; }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> v) { this.allowedOrigins = v; }
    }

    public static class Correlation {
        private String headerName = "X-Correlation-Id";
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String v) { this.headerName = v; }
    }
}
