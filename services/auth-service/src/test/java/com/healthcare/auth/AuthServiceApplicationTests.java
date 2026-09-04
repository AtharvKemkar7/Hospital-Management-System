package com.healthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring application context loads cleanly with the
 * {@code test} profile. This catches wiring problems (missing beans,
 * circular dependencies, misconfigured auto-configuration) without
 * needing a database.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
        // intentionally empty: success means all beans wire correctly
    }
}
