package com.hsj.dto.order;

import com.hsj.entity.OrderHistory;
import com.hsj.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderHistoryResponse {

    private Long id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime createdAt;

    public static OrderHistoryResponse from(OrderHistory history) {
        return OrderHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
