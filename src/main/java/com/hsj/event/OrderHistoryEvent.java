package com.hsj.event;

import com.hsj.entity.enums.OrderStatus;
import lombok.Getter;

/**
 * 주문 상태 변경·생성·취소 후 이력을 비동기로 기록하기 위한 이벤트.
 * 트랜잭션 커밋 이후 별도 스레드에서 처리되므로 주문 트랜잭션에 영향을 주지 않는다.
 */
@Getter
public class OrderHistoryEvent {

    private final Long orderId;
    private final OrderStatus previousStatus;   // nullable (최초 생성 시)
    private final OrderStatus newStatus;
    private final String reason;
    private final String changedBy;

    public OrderHistoryEvent(Long orderId, OrderStatus previousStatus, OrderStatus newStatus,
                             String reason, String changedBy) {
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }
}
