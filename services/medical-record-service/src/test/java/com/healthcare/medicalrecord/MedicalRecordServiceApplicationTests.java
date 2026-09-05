package com.healthcare.medicalrecord;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MedicalRecordServiceApplicationTests {

    @Test
    void contextLoads() {
        // success means all beans wire correctly
    }
}
