package com.healthcare.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Beans used by the rest of the service.
 *
 * <p>{@link Clock} is a Spring bean so the entire codebase can read the
 * current time without {@code new Date()} or {@code Instant.now()}, which
 * makes tests deterministic.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder(AuthProperties props) {
        int cost = props.getSecurity().getBcryptCost();
        return new BCryptPasswordEncoder(cost);
    }

    @Bean
    public Clock clock() {
        // Always UTC. See docs/architecture.md §4.10.
        return Clock.system(ZoneOffset.UTC);
    }
}
