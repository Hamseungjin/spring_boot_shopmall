package com.hsj.entity;

import com.hsj.entity.enums.OrderItemStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    // ──── 주문 시점 스냅샷 필드 ────

    @Column(name = "snapshot_product_name", nullable = false, length = 200)
    private String snapshotProductName;

    @Column(name = "snapshot_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal snapshotPrice;

    @Column(name = "snapshot_image_url", length = 500)
    private String snapshotImageUrl;

    @Column(name = "snapshot_description", columnDefinition = "TEXT")
    private String snapshotDescription;

    // ──── 주문 아이템 단위 상태 (부분 취소/배송) ────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderItemStatus status = OrderItemStatus.PENDING_PAYMENT;

    @Builder
    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.status = OrderItemStatus.PENDING_PAYMENT;
        captureSnapshot(product);
    }

    private void captureSnapshot(Product product) {
        this.snapshotProductName = product.getName();
        this.snapshotPrice = product.getPrice();
        this.snapshotImageUrl = product.getImageUrl();
        this.snapshotDescription = product.getDescription();
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getSubtotal() {
        return this.snapshotPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    public void changeStatus(OrderItemStatus newStatus) {
        this.status = this.status.transitionTo(newStatus);
    }

    public boolean isCancellable() {
        return this.status.canTransitionTo(OrderItemStatus.CANCELLED);
    }
}
