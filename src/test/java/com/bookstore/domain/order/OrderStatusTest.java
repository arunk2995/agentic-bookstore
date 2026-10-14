package com.bookstore.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderStatus State Machine Tests")
class OrderStatusTest {

    @Test
    @DisplayName("PENDING can transition to INVENTORY_RESERVED or CANCELLED")
    void pending_canTransitionToReservedOrCancelled() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.INVENTORY_RESERVED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("PENDING cannot skip to CONFIRMED")
    void pending_cannotSkipToConfirmed() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }

    @Test
    @DisplayName("DELIVERED and CANCELLED are final states")
    void deliveredAndCancelled_areFinal() {
        assertThat(OrderStatus.DELIVERED.isFinal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isFinal()).isTrue();
        assertThat(OrderStatus.REFUNDED.isFinal()).isTrue();
        assertThat(OrderStatus.PENDING.isFinal()).isFalse();
    }

    @Test
    @DisplayName("isCancellable returns true for early statuses")
    void isCancellable_earlyStatuses_returnsTrue() {
        assertThat(OrderStatus.PENDING.isCancellable()).isTrue();
        assertThat(OrderStatus.INVENTORY_RESERVED.isCancellable()).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.isCancellable()).isTrue();
        assertThat(OrderStatus.CONFIRMED.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("isCancellable returns false for late statuses")
    void isCancellable_lateStatuses_returnsFalse() {
        assertThat(OrderStatus.SHIPPED.isCancellable()).isFalse();
        assertThat(OrderStatus.DELIVERED.isCancellable()).isFalse();
        assertThat(OrderStatus.CANCELLED.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("Order transitionTo throws on invalid transition")
    void order_transitionTo_invalidTransition_throws() {
        Order order = Order.builder()
            .id(UUID.randomUUID())
            .orderNumber("ORD-001")
            .status(OrderStatus.SHIPPED)
            .subtotal(BigDecimal.valueOf(500))
            .totalAmount(BigDecimal.valueOf(500))
            .discountAmount(BigDecimal.ZERO)
            .shippingAmount(BigDecimal.ZERO)
            .taxAmount(BigDecimal.ZERO)
            .currency("INR")
            .build();

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PENDING))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Full happy path transition works end to end")
    void orderStatus_fullHappyPath() {
        // PENDING -> INVENTORY_RESERVED -> PAYMENT_PENDING -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.INVENTORY_RESERVED)).isTrue();
        assertThat(OrderStatus.INVENTORY_RESERVED.canTransitionTo(OrderStatus.PAYMENT_PENDING)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }
}
