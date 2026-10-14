package com.bookstore.controller;

import com.bookstore.dto.inventory.*;
import com.bookstore.security.BookstoreUserPrincipal;
import com.bookstore.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventory", description = "Inventory management (Admin/Inventory Manager only)")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{bookId}")
    @Operation(summary = "Get inventory for a book")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable UUID bookId) {
        return ResponseEntity.ok(inventoryService.getInventory(bookId));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock books")
    public ResponseEntity<List<InventoryResponse>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(inventoryService.getLowStockBooks(threshold));
    }

    @GetMapping("/{bookId}/movements")
    @Operation(summary = "Get inventory movement history")
    public ResponseEntity<Page<InventoryMovementResponse>> getMovements(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getMovements(bookId, page, size));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Adjust inventory for a book")
    public ResponseEntity<InventoryResponse> adjustInventory(
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @RequestParam UUID bookId,
            @Valid @RequestBody AdjustInventoryRequest request) {
        inventoryService.adjustInventory(bookId, request.getQuantityChange(),
            request.getReason(), principal.getUserId());
        return ResponseEntity.ok(inventoryService.getInventory(bookId));
    }
}
