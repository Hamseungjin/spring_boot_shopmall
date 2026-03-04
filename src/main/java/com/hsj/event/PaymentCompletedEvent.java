package com.hsj.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 결제 완료 후 후처리(알림 발송, 마케팅 이벤트 등)를 비동기로 수행하기 위한 이벤트.
 * 향후 이메일/SMS 알림, 포인트 적립 트리거 등을 이 이벤트 핸들러에 추가한다.
 */
@Getter
public class PaymentCompletedEvent {

    private final Long paymentId;
    private final Long orderId;
    private final BigDecimal amount;
    private final String idempotencyKey;

    public PaymentCompletedEvent(Long paymentId, Long orderId, BigDecimal amount, String idempotencyKey) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
