package com.bookstore.agent.tools;

import com.bookstore.agent.core.*;
import com.bookstore.domain.order.OrderStatus;
import com.bookstore.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tools for the Order Assistance Agent.
 *
 * SECURITY: User identity is always derived from the tool's SecurityContext,
 * not from agent arguments. The LLM cannot impersonate another user.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderAssistanceTools {

    private final ToolRegistry toolRegistry;
    private final OrderService orderService;

    @PostConstruct
    public void registerTools() {
        toolRegistry.register(
            ToolDefinition.builder()
                .name("getMyOrders")
                .description("Get the authenticated customer's order history")
                .parameters(Map.of())
                .requiredRoles(List.of())
                .requiresAuth(true)
                .build(),
            this::getMyOrders
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("getMyOrderDetails")
                .description("Get details for a specific order belonging to the authenticated customer")
                .parameters(Map.of(
                    "orderId", ToolDefinition.ToolParameter.builder().type("string").description("Order UUID").required(true).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(true)
                .build(),
            this::getMyOrderDetails
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("getOrderStatus")
                .description("Get the current status of an order")
                .parameters(Map.of(
                    "orderId", ToolDefinition.ToolParameter.builder().type("string").description("Order UUID").required(true).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(true)
                .build(),
            this::getOrderStatus
        );

        toolRegistry.register(
            ToolDefinition.builder()
                .name("requestOrderCancellation")
                .description("Request cancellation of an order — subject to status validation")
                .parameters(Map.of(
                    "orderId", ToolDefinition.ToolParameter.builder().type("string").description("Order UUID").required(true).build(),
                    "reason", ToolDefinition.ToolParameter.builder().type("string").description("Reason for cancellation").required(false).build()
                ))
                .requiredRoles(List.of())
                .requiresAuth(true)
                .requiresConfirmation(true)
                .build(),
            this::requestOrderCancellation
        );
    }

    /**
     * User ID is always derived from the security context thread-local,
     * never from the LLM's arguments.
     */
    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof com.bookstore.security.BookstoreUserPrincipal p) {
            return p.getUserId();
        }
        return null;
    }

    private ToolCallResult getMyOrders(ToolCallRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) return ToolCallResult.error("UNAUTHORIZED", "Not authenticated");
        var page = orderService.getUserOrders(userId, 0, 10);
        return ToolCallResult.success(Map.of(
            "orders", page.getContent(),
            "totalOrders", page.getTotalElements()
        ));
    }

    private ToolCallResult getMyOrderDetails(ToolCallRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) return ToolCallResult.error("UNAUTHORIZED", "Not authenticated");
        String orderId = getStr(request.getArguments(), "orderId");
        if (orderId == null) return ToolCallResult.error("MISSING_PARAM", "orderId required");
        // Always pass userId to enforce ownership check
        var order = orderService.getOrder(UUID.fromString(orderId), userId, false);
        return ToolCallResult.success(order);
    }

    private ToolCallResult getOrderStatus(ToolCallRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) return ToolCallResult.error("UNAUTHORIZED", "Not authenticated");
        String orderId = getStr(request.getArguments(), "orderId");
        if (orderId == null) return ToolCallResult.error("MISSING_PARAM", "orderId required");
        var order = orderService.getOrder(UUID.fromString(orderId), userId, false);
        return ToolCallResult.success(Map.of(
            "orderId", orderId,
            "orderNumber", order.getOrderNumber(),
            "status", order.getStatus()
        ));
    }

    private ToolCallResult requestOrderCancellation(ToolCallRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) return ToolCallResult.error("UNAUTHORIZED", "Not authenticated");
        String orderId = getStr(request.getArguments(), "orderId");
        if (orderId == null) return ToolCallResult.error("MISSING_PARAM", "orderId required");
        String reason = getStr(request.getArguments(), "reason");
        if (reason == null) reason = "Customer requested cancellation via agent";

        // Validate order belongs to user and is cancellable
        var order = orderService.getOrder(UUID.fromString(orderId), userId, false);
        if (!order.getStatus().isCancellable()) {
            return ToolCallResult.error("INVALID_OPERATION",
                "Order " + order.getOrderNumber() + " cannot be cancelled in status: " + order.getStatus());
        }

        var cancelled = orderService.cancelOrder(UUID.fromString(orderId), userId, reason, false);
        return ToolCallResult.success(Map.of(
            "orderId", orderId,
            "orderNumber", cancelled.getOrderNumber(),
            "status", cancelled.getStatus(),
            "message", "Order has been cancelled successfully"
        ));
    }

    private String getStr(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object v = args.get(key);
        return v != null ? v.toString() : null;
    }
}
