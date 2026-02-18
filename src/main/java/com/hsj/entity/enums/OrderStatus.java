package com.hsj.entity.enums;

import java.util.Set;

public enum OrderStatus {

    PENDING_PAYMENT(Set.of()),
    PAID(Set.of()),
    PREPARING(Set.of()),
    SHIPPED(Set.of()),
    DELIVERED(Set.of()),
    CANCELLED(Set.of()),
    REFUND_REQUESTED(Set.of()),
    REFUNDED(Set.of());

    private Set<OrderStatus> allowedTransitions;

    OrderStatus(Set<OrderStatus> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    static {
        PENDING_PAYMENT.allowedTransitions = Set.of(PAID, CANCELLED);
        PAID.allowedTransitions = Set.of(PREPARING, CANCELLED, REFUND_REQUESTED);
        PREPARING.allowedTransitions = Set.of(SHIPPED, CANCELLED);
        SHIPPED.allowedTransitions = Set.of(DELIVERED);
        DELIVERED.allowedTransitions = Set.of(REFUND_REQUESTED);
        CANCELLED.allowedTransitions = Set.of();
        REFUND_REQUESTED.allowedTransitions = Set.of(REFUNDED);
        REFUNDED.allowedTransitions = Set.of();
    }

    public boolean canTransitionTo(OrderStatus target) {
        return this.allowedTransitions.contains(target);
    }

    public OrderStatus transitionTo(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    String.format("상태 전이 불가: %s → %s", this.name(), target.name())
            );
        }
        return target;
    }
}
