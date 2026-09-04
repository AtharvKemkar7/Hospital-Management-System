package com.healthcare.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BillingServiceApplicationTests {

    @Test
    void contextLoads() {
        // success means all beans wire correctly
    }
}
