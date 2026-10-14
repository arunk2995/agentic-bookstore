package com.bookstore.integration.email;

public interface EmailService {
    void sendOrderConfirmation(String toEmail, String customerName, String orderNumber, String summary);
    void sendPasswordReset(String toEmail, String customerName, String resetToken);
    void sendLowStockAlert(String toEmail, String bookTitle, int quantity);
    void sendOrderStatusUpdate(String toEmail, String customerName, String orderNumber, String status);
}
