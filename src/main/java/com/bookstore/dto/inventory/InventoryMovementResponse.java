package com.bookstore.dto.inventory;

import com.bookstore.domain.inventory.MovementType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryMovementResponse {
    private UUID id;
    private UUID bookId;
    private MovementType movementType;
    private int quantityChange;
    private int quantityBefore;
    private int quantityAfter;
    private String reason;
    private String referenceId;
    private Instant createdAt;
}
