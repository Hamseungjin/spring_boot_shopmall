package com.hsj.dto.order;

import com.hsj.entity.Order;
import com.hsj.entity.OrderItem;
import com.hsj.entity.enums.OrderItemStatus;
import com.hsj.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class OrderItemResponse {
        private Long orderItemId;
        private Long productId;
        private String snapshotProductName;
        private BigDecimal snapshotPrice;
        private int quantity;
        private BigDecimal subtotal;
        private OrderItemStatus status;

        public static OrderItemResponse from(OrderItem item) {
            return OrderItemResponse.builder()
                    .orderItemId(item.getId())
                    .productId(item.getProduct().getId())
                    .snapshotProductName(item.getSnapshotProductName())
                    .snapshotPrice(item.getSnapshotPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .status(item.getStatus())
                    .build();
        }
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
