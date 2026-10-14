package com.bookstore.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartResponse {
    private UUID id;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private String currency;
    private int totalItems;
    private Instant updatedAt;

    @Data
    @Builder
    public static class CartItemResponse {
        private UUID id;
        private UUID bookId;
        private String bookTitle;
        private String bookIsbn;
        private String coverImageUrl;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String currency;
        private int availableQuantity;
    }
}
