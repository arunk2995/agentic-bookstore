package com.bookstore.integration.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "bookstore.email.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockEmailService implements EmailService {

    @Override
    public void sendOrderConfirmation(String toEmail, String customerName, String orderNumber, String summary) {
        log.info("[MOCK EMAIL] Order confirmation to {} for order {}", toEmail, orderNumber);
    }

    @Override
    public void sendPasswordReset(String toEmail, String customerName, String resetToken) {
        log.info("[MOCK EMAIL] Password reset to {} - token: {}", toEmail, resetToken);
    }

    @Override
    public void sendLowStockAlert(String toEmail, String bookTitle, int quantity) {
        log.info("[MOCK EMAIL] Low stock alert to {} - book: {} qty: {}", toEmail, bookTitle, quantity);
    }

    @Override
    public void sendOrderStatusUpdate(String toEmail, String customerName, String orderNumber, String status) {
        log.info("[MOCK EMAIL] Status update to {} - order: {} status: {}", toEmail, orderNumber, status);
    }
}
