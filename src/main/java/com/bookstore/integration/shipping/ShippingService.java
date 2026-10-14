package com.bookstore.integration.shipping;

public interface ShippingService {
    ShipmentResult createShipment(ShipmentRequest request);
    String getTrackingUrl(String trackingNumber);

    record ShipmentRequest(
        String orderNumber,
        String recipientName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country
    ) {}

    record ShipmentResult(
        boolean success,
        String trackingNumber,
        String carrier,
        String estimatedDelivery,
        String failureReason
    ) {}
}
