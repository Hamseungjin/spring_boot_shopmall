package com.hsj.dto.order;

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
    }
}
