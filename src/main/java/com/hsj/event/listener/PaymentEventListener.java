package com.hsj.event.listener;

import com.hsj.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 완료 후처리 비동기 리스너.
 *
 * <pre>
 * 현재: 결제 완료 로그 출력
 * 향후 확장 포인트:
 *   - 결제 완료 이메일 / SMS 발송
 *   - 포인트 적립 이벤트 트리거
 *   - 마케팅 플랫폼 연동
 *   - 물류 시스템 배송 접수
 * </pre>
 */
@Slf4j
@Component
public class PaymentEventListener {

    @Async("eventLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentCompletedEvent event) {
        try {
            // TODO: 결제 완료 알림 발송 (이메일/SMS), 포인트 적립 연동 등 추가
            log.info("결제 완료 후처리: paymentId={}, orderId={}, amount={}, key={}",
                    event.getPaymentId(), event.getOrderId(),
                    event.getAmount(), event.getIdempotencyKey());
        } catch (Exception e) {
            log.error("결제 완료 후처리 실패: paymentId={}, orderId={} - {}",
                    event.getPaymentId(), event.getOrderId(), e.getMessage(), e);
        }
    }
}
