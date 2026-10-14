package com.bookstore.dto.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable;
    private int reorderPoint;
    private boolean lowStock;
    private Instant updatedAt;
}
