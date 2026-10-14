package com.bookstore.integration.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    PaymentResult initiatePayment(UUID orderId, BigDecimal amount, String currency,
                                  String paymentToken, String idempotencyKey);
    PaymentResult capturePayment(String providerReference);
    PaymentResult refundPayment(String providerReference, BigDecimal amount, String reason);
    boolean verifyWebhookSignature(String payload, String signature);

    record PaymentResult(
        boolean success,
        String providerReference,
        String status,
        String failureReason
    ) {}
}
