package com.payment.processing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentProcessingApplicationTests {

    @Test
    void contextLoads() {
        // Simple test to verify application context loads
    }

    @Test
    void mainMethodRuns() {
        // Verify main method doesn't throw
        PaymentProcessingApplication.main(new String[]{});
    }
}
