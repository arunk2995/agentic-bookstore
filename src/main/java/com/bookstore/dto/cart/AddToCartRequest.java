package com.bookstore.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddToCartRequest {
    @NotNull(message = "Book ID is required")
    private UUID bookId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
