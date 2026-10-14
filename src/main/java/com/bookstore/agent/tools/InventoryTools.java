package com.bookstore.agent.tools;

import com.bookstore.agent.core.*;
import com.bookstore.service.InventoryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool implementations for the Inventory Operations Agent.
 * Stock adjustments are proposed only — not executed directly by the LLM.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryTools {

    private final ToolRegistry toolRegistry;
    private final InventoryService inventoryService;

    @PostConstruct
    public void registerTools() {
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getInventory")
                .description("Get current inventory status for a specific book")
                .parameters(Map.of(
                    "bookId", ToolDefinition.ToolParameter.builder().type("string").description("Book UUID").required(true).build()
                ))
                .requiredRoles(List.of("INVENTORY_MANAGER", "ADMIN"))
                .requiresAuth(true)
                .build(),
            this::getInventory
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("getLowStockBooks")
                .description("Get list of books with stock below a threshold")
                .parameters(Map.of(
                    "threshold", ToolDefinition.ToolParameter.builder().type("integer").description("Stock threshold").required(false).build()
                ))
                .requiredRoles(List.of("INVENTORY_MANAGER", "ADMIN"))
                .requiresAuth(true)
                .build(),
            this::getLowStockBooks
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("getInventoryMovements")
                .description("Get inventory movement history for a book")
                .parameters(Map.of(
                    "bookId", ToolDefinition.ToolParameter.builder().type("string").description("Book UUID").required(true).build()
                ))
                .requiredRoles(List.of("INVENTORY_MANAGER", "ADMIN"))
                .requiresAuth(true)
                .build(),
            this::getInventoryMovements
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("proposeStockAdjustment")
                .description("Propose a stock adjustment for review — does NOT execute the adjustment")
                .parameters(Map.of(
                    "bookId", ToolDefinition.ToolParameter.builder().type("string").description("Book UUID").required(true).build(),
                    "quantity", ToolDefinition.ToolParameter.builder().type("integer").description("Quantity change (positive=add, negative=remove)").required(true).build(),
                    "reason", ToolDefinition.ToolParameter.builder().type("string").description("Reason for adjustment").required(true).build()
                ))
                .requiredRoles(List.of("INVENTORY_MANAGER", "ADMIN"))
                .requiresAuth(true)
                .requiresConfirmation(true) // Requires explicit confirmation
                .build(),
            this::proposeStockAdjustment
        );
    }

    private ToolCallResult getInventory(ToolCallRequest request) {
        String bookId = getStr(request.getArguments(), "bookId");
        if (bookId == null) return ToolCallResult.error("MISSING_PARAM", "bookId required");
        return ToolCallResult.success(inventoryService.getInventory(UUID.fromString(bookId)));
    }

    private ToolCallResult getLowStockBooks(ToolCallRequest request) {
        int threshold = getInt(request.getArguments(), "threshold", 10);
        return ToolCallResult.success(inventoryService.getLowStockBooks(threshold));
    }

    private ToolCallResult getInventoryMovements(ToolCallRequest request) {
        String bookId = getStr(request.getArguments(), "bookId");
        if (bookId == null) return ToolCallResult.error("MISSING_PARAM", "bookId required");
        var movements = inventoryService.getMovements(UUID.fromString(bookId), 0, 20);
        return ToolCallResult.success(Map.of(
            "bookId", bookId,
            "movements", movements.getContent(),
            "total", movements.getTotalElements()
        ));
    }

    private ToolCallResult proposeStockAdjustment(ToolCallRequest request) {
        // This only PROPOSES — it does not execute. Confirmation required via separate API.
        String bookId = getStr(request.getArguments(), "bookId");
        int quantity = getInt(request.getArguments(), "quantity", 0);
        String reason = getStr(request.getArguments(), "reason");

        var inventory = inventoryService.getInventory(UUID.fromString(bookId));

        return ToolCallResult.success(Map.of(
            "proposal", Map.of(
                "bookId", bookId,
                "bookTitle", inventory.getBookTitle(),
                "currentStock", inventory.getQuantityAvailable(),
                "proposedChange", quantity,
                "projectedStock", inventory.getQuantityAvailable() + quantity,
                "reason", reason != null ? reason : "",
                "status", "PENDING_CONFIRMATION",
                "message", "This adjustment requires explicit confirmation via POST /api/v1/inventory/adjustments"
            )
        ));
    }

    private String getStr(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }

    private int getInt(Map<String, Object> args, String key, int def) {
        if (args == null) return def;
        Object v = args.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
}
