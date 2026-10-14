package com.bookstore.integration.shipping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "bookstore.shipping.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockShippingService implements ShippingService {

    @Override
    public ShipmentResult createShipment(ShipmentRequest request) {
        log.info("[MOCK SHIPPING] Creating shipment for order {}", request.orderNumber());
        String tracking = "MOCK-SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new ShipmentResult(true, tracking, "MockCarrier", "5-7 business days", null);
    }

    @Override
    public String getTrackingUrl(String trackingNumber) {
        return "https://tracking.example.com/" + trackingNumber;
    }
}
