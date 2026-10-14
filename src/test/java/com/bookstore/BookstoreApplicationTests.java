package com.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "bookstore.jwt.secret=test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
    "bookstore.agent.mock-mode=true",
    "bookstore.payment.mock-mode=true",
    "bookstore.email.mock-mode=true",
    "bookstore.shipping.mock-mode=true"
})
class BookstoreApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context loads without errors
    }
}
