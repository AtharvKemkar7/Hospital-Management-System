package com.healthcare.prescription;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PrescriptionServiceApplicationTests {

    @Test
    void contextLoads() {
        // success means all beans wire correctly
    }
}
