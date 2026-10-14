package com.bookstore.controller;

import com.bookstore.dto.order.*;
import com.bookstore.security.BookstoreUserPrincipal;
import com.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order management and checkout")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order (checkout)")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.createOrder(principal.getUserId(), request));
    }

    @GetMapping
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getUserOrders(principal.getUserId(), page, size));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @PathVariable UUID orderId) {
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.getOrder(orderId, principal.getUserId(), isAdmin));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal BookstoreUserPrincipal principal,
            @PathVariable UUID orderId,
            @RequestParam(required = false, defaultValue = "Customer requested cancellation") String reason) {
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.cancelOrder(orderId, principal.getUserId(), reason, isAdmin));
    }
}
