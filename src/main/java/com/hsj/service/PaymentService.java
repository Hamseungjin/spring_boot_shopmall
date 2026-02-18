package com.hsj.service;

import com.hsj.dto.order.OrderStatusChangeRequest;
import com.hsj.dto.payment.PaymentRequest;
import com.hsj.dto.payment.PaymentResponse;
import com.hsj.entity.Order;
import com.hsj.entity.Payment;
import com.hsj.entity.enums.OrderStatus;
import com.hsj.entity.enums.PaymentStatus;
import com.hsj.exception.BusinessException;
import com.hsj.exception.DuplicateException;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.OrderRepository;
import com.hsj.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String paidBy) {
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                log.info("멱등성 키 중복 감지 (완료된 결제 반환): key={}", request.getIdempotencyKey());
                return PaymentResponse.from(existing);
            }
            if (existing.getStatus() == PaymentStatus.PENDING) {
                throw new DuplicateException(ErrorCode.DUPLICATE_PAYMENT,
                        "이미 처리 중인 결제 요청입니다.");
            }
        }

        Order order = orderRepository.findByIdAndDeletedFalse(request.getOrderId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "결제 대기 상태의 주문만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
        }

        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(order.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();

        payment = paymentRepository.save(payment);

        try {
            processExternalPayment(payment);
            payment.complete();

            OrderStatusChangeRequest statusChange = OrderStatusChangeRequest.of(
                    OrderStatus.PAID, "결제 완료");
            orderService.changeOrderStatus(order.getId(), statusChange, paidBy);

            log.info("결제 완료: paymentId={}, orderId={}, amount={}, key={}",
                    payment.getId(), order.getId(), payment.getAmount(), payment.getIdempotencyKey());

        } catch (Exception e) {
            payment.fail();
            log.error("결제 실패: orderId={}, key={}", order.getId(), request.getIdempotencyKey(), e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "결제 처리 중 오류가 발생했습니다.");
        }

        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long orderId, String cancelledBy) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "완료된 결제만 취소할 수 있습니다.");
        }

        payment.cancel();
        log.info("결제 취소: paymentId={}, orderId={}", payment.getId(), orderId);
        return PaymentResponse.from(payment);
    }

    private void processExternalPayment(Payment payment) {
        log.info("외부 PG 결제 처리 (시뮬레이션): amount={}, method={}",
                payment.getAmount(), payment.getPaymentMethod());
    }
}
