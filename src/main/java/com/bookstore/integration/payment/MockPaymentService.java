package com.bookstore.integration.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "bookstore.payment.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockPaymentService implements PaymentService {

    @Override
    public PaymentResult initiatePayment(UUID orderId, BigDecimal amount, String currency,
                                         String paymentToken, String idempotencyKey) {
        log.info("[MOCK] Initiating payment for order {} amount={} {}", orderId, amount, currency);
        // Simulate payment success for test tokens, failure for "fail" token
        if ("fail".equalsIgnoreCase(paymentToken)) {
            return new PaymentResult(false, null, "FAILED", "Mock payment failure");
        }
        String mockRef = "MOCK-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(true, mockRef, "COMPLETED", null);
    }

    @Override
    public PaymentResult capturePayment(String providerReference) {
        log.info("[MOCK] Capturing payment: {}", providerReference);
        return new PaymentResult(true, providerReference, "COMPLETED", null);
    }

    @Override
    public PaymentResult refundPayment(String providerReference, BigDecimal amount, String reason) {
        log.info("[MOCK] Refunding payment: {} amount={}", providerReference, amount);
        return new PaymentResult(true, providerReference, "REFUNDED", null);
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        log.info("[MOCK] Webhook signature verification (always passes in mock mode)");
        return true;
    }
}
