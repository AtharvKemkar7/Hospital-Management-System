package com.healthcare.doctor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DoctorServiceApplicationTests {

    @Test
    void contextLoads() {
        // success means all beans wire correctly
    }
}
