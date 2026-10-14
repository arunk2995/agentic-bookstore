package com.bookstore.dto.order;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String shippingAddressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String shippingAddressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String shippingCity;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String shippingState;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String shippingPostalCode;

    @Size(min = 2, max = 2, message = "Country must be a 2-character ISO code")
    private String shippingCountry;

    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private String paymentToken;
}
