package com.hsj.event.listener;

import com.hsj.entity.Order;
import com.hsj.entity.OrderHistory;
import com.hsj.event.OrderHistoryEvent;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.OrderHistoryRepository;
import com.hsj.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 이력 비동기 저장 리스너.
 *
 * <pre>
 * - AFTER_COMMIT: 주문 트랜잭션이 정상 커밋된 후에만 실행 → 롤백 시 이력 저장 시도 없음
 * - @Async: 별도 스레드(orderHistoryExecutor)에서 실행 → 호출자 응답 지연 없음
 * - REQUIRES_NEW: 독립 트랜잭션으로 이력 저장 → 실패해도 주문 트랜잭션에 영향 없음
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderHistoryEventListener {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    @Async("orderHistoryExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OrderHistoryEvent event) {
        try {
            Order order = orderRepository.findByIdAndDeletedFalse(event.getOrderId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

            OrderHistory history = OrderHistory.record(
                    order,
                    event.getPreviousStatus(),
                    event.getNewStatus(),
                    event.getReason(),
                    event.getChangedBy()
            );
            orderHistoryRepository.save(history);

            log.debug("주문 이력 저장 완료: orderId={}, {} → {}",
                    event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

        } catch (Exception e) {
            log.error("주문 이력 저장 실패 (수동 재처리 필요): orderId={}, {} → {}, reason={}",
                    event.getOrderId(), event.getPreviousStatus(), event.getNewStatus(),
                    event.getReason(), e);
        }
    }
}
