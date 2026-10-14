package com.bookstore.domain.order;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> Set.of(INVENTORY_RESERVED, CANCELLED).contains(next);
            case INVENTORY_RESERVED -> Set.of(PAYMENT_PENDING, CANCELLED).contains(next);
            case PAYMENT_PENDING -> Set.of(CONFIRMED, CANCELLED).contains(next);
            case CONFIRMED -> Set.of(PROCESSING, CANCELLED).contains(next);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED).contains(next);
            case SHIPPED -> Set.of(DELIVERED).contains(next);
            case DELIVERED -> Set.of(REFUNDED).contains(next);
            case CANCELLED, REFUNDED -> false;
        };
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }

    public boolean isCancellable() {
        return Set.of(PENDING, INVENTORY_RESERVED, PAYMENT_PENDING, CONFIRMED).contains(this);
    }
}
