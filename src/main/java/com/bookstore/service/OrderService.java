package com.bookstore.service;

import com.bookstore.domain.cart.Cart;
import com.bookstore.domain.cart.CartItem;
import com.bookstore.domain.order.*;
import com.bookstore.domain.payment.*;
import com.bookstore.domain.user.User;
import com.bookstore.dto.order.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.integration.email.EmailService;
import com.bookstore.integration.payment.PaymentService;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final CartService cartService;

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            orderRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw BookstoreException.conflict(
                        "Order already exists for idempotency key: " + request.getIdempotencyKey());
                });
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> BookstoreException.notFound("User", userId));

        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> BookstoreException.badRequest("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw BookstoreException.badRequest("Cannot create order from empty cart");
        }

        // Build order items from cart (server-side price validation)
        List<OrderItem> orderItems = cart.getItems().stream()
            .map(cartItem -> buildOrderItem(cartItem))
            .toList();

        BigDecimal subtotal = orderItems.stream()
            .map(OrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = subtotal; // shipping/tax can be added here

        String orderNumber = "ORD-" + System.currentTimeMillis();

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .user(user)
            .status(OrderStatus.PENDING)
            .subtotal(subtotal)
            .discountAmount(BigDecimal.ZERO)
            .shippingAmount(BigDecimal.ZERO)
            .taxAmount(BigDecimal.ZERO)
            .totalAmount(totalAmount)
            .currency("INR")
            .idempotencyKey(request.getIdempotencyKey())
            .shippingAddressLine1(request.getShippingAddressLine1())
            .shippingAddressLine2(request.getShippingAddressLine2())
            .shippingCity(request.getShippingCity())
            .shippingState(request.getShippingState())
            .shippingPostalCode(request.getShippingPostalCode())
            .shippingCountry(request.getShippingCountry())
            .notes(request.getNotes())
            .build();

        // Link items to order
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.getItems().addAll(orderItems);

        order = orderRepository.save(order);

        // Step 1: Reserve inventory
        try {
            for (CartItem cartItem : cart.getItems()) {
                inventoryService.reserveStock(cartItem.getBook().getId(), order.getId(), cartItem.getQuantity());
            }
            order.transitionTo(OrderStatus.INVENTORY_RESERVED);
            order = orderRepository.save(order);
        } catch (Exception e) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledReason("Inventory reservation failed: " + e.getMessage());
            orderRepository.save(order);
            throw e;
        }

        // Step 2: Process payment
        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        order = orderRepository.save(order);

        String paymentIdempotencyKey = order.getId().toString();
        PaymentService.PaymentResult paymentResult = paymentService.initiatePayment(
            order.getId(), totalAmount, "INR", request.getPaymentToken(), paymentIdempotencyKey);

        Payment payment = Payment.builder()
            .order(order)
            .amount(totalAmount)
            .currency("INR")
            .idempotencyKey(paymentIdempotencyKey)
            .build();

        if (paymentResult.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProviderReference(paymentResult.providerReference());
            payment.setCompletedAt(Instant.now());
            paymentRepository.save(payment);

            order.transitionTo(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);

            // Fulfill inventory
            inventoryService.fulfillReservation(order.getId());

            // Clear cart
            cartService.clearCart(userId);

            // Notify customer
            try {
                emailService.sendOrderConfirmation(
                    user.getEmail(),
                    user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                    order.getOrderNumber(),
                    "Order total: " + totalAmount + " INR");
            } catch (Exception e) {
                log.warn("Failed to send order confirmation email: {}", e.getMessage());
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(paymentResult.failureReason());
            paymentRepository.save(payment);

            // Release reservation
            inventoryService.releaseReservation(order.getId());
            order.transitionTo(OrderStatus.CANCELLED);
            order.setCancelledReason("Payment failed: " + paymentResult.failureReason());
            order = orderRepository.save(order);
        }

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID userId, boolean isAdmin) {
        Order order;
        if (isAdmin) {
            order = orderRepository.findById(orderId)
                .orElseThrow(() -> BookstoreException.notFound("Order", orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> BookstoreException.notFound("Order", orderId));
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId, String reason, boolean isAdmin) {
        Order order;
        if (isAdmin) {
            order = orderRepository.findById(orderId)
                .orElseThrow(() -> BookstoreException.notFound("Order", orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> BookstoreException.notFound("Order", orderId));
        }

        if (!order.getStatus().isCancellable()) {
            throw BookstoreException.invalidOperation(
                "Order cannot be cancelled in status: " + order.getStatus());
        }

        // Release inventory if reserved
        if (order.getStatus() == OrderStatus.INVENTORY_RESERVED ||
            order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            inventoryService.releaseReservation(order.getId());
        }

        order.transitionTo(OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        order = orderRepository.save(order);

        log.info("Order {} cancelled. Reason: {}", orderId, reason);
        return toResponse(order);
    }

    private OrderItem buildOrderItem(CartItem cartItem) {
        // Server-side price validation - always use catalog price, not cart price
        BigDecimal currentPrice = cartItem.getBook().getEffectivePrice();
        BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return OrderItem.builder()
            .book(cartItem.getBook())
            .quantity(cartItem.getQuantity())
            .unitPrice(currentPrice)
            .discountAmount(BigDecimal.ZERO)
            .lineTotal(lineTotal)
            .currency(cartItem.getCurrency())
            .bookTitleSnapshot(cartItem.getBook().getTitle())
            .bookIsbnSnapshot(cartItem.getBook().getIsbn())
            .build();
    }

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
            .map(item -> OrderResponse.OrderItemResponse.builder()
                .id(item.getId())
                .bookId(item.getBook().getId())
                .bookTitle(item.getBookTitleSnapshot())
                .bookIsbn(item.getBookIsbnSnapshot())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountAmount(item.getDiscountAmount())
                .lineTotal(item.getLineTotal())
                .build())
            .toList();

        return OrderResponse.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .status(order.getStatus())
            .items(items)
            .subtotal(order.getSubtotal())
            .discountAmount(order.getDiscountAmount())
            .shippingAmount(order.getShippingAmount())
            .taxAmount(order.getTaxAmount())
            .totalAmount(order.getTotalAmount())
            .currency(order.getCurrency())
            .shippingAddressLine1(order.getShippingAddressLine1())
            .shippingAddressLine2(order.getShippingAddressLine2())
            .shippingCity(order.getShippingCity())
            .shippingState(order.getShippingState())
            .shippingPostalCode(order.getShippingPostalCode())
            .shippingCountry(order.getShippingCountry())
            .notes(order.getNotes())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}
