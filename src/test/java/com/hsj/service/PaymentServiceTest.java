package com.hsj.service;

import com.hsj.dto.order.OrderStatusChangeRequest;
import com.hsj.dto.payment.PaymentRequest;
import com.hsj.dto.payment.PaymentResponse;
import com.hsj.entity.Member;
import com.hsj.entity.Order;
import com.hsj.entity.Payment;
import com.hsj.entity.enums.MemberRole;
import com.hsj.entity.enums.OrderStatus;
import com.hsj.entity.enums.PaymentStatus;
import com.hsj.exception.BusinessException;
import com.hsj.exception.DuplicateException;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.OrderRepository;
import com.hsj.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    private Member member;
    private Order order;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("buyer@test.com")
                .password("encoded_pw")
                .name("구매자")
                .role(MemberRole.CUSTOMER)
                .build();

        order = Order.builder()
                .member(member)
                .shippingAddress("서울시 강남구")
                .receiverName("홍길동")
                .receiverPhone("010-0000-0000")
                .build();
        // Order.totalAmount 를 테스트용으로 세팅
        ReflectionTestUtils.setField(order, "totalAmount", BigDecimal.valueOf(30_000));

        paymentRequest = new PaymentRequest();
        ReflectionTestUtils.setField(paymentRequest, "orderId", 1L);
        ReflectionTestUtils.setField(paymentRequest, "idempotencyKey", "IDEM-KEY-001");
        ReflectionTestUtils.setField(paymentRequest, "paymentMethod", "CARD");
    }

    // ═══════════════════════ processPayment ═══════════════════════

    @Test
    @DisplayName("processPayment: 정상 결제 처리 - Payment가 COMPLETED 상태로 저장된다")
    void processPayment_정상결제() {
        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001")).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        // save()는 넘겨받은 Payment 객체 그대로 반환
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderService.changeOrderStatus(anyLong(), any(OrderStatusChangeRequest.class), anyString()))
                .thenReturn(null);

        PaymentResponse response = paymentService.processPayment(paymentRequest, "buyer@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        verify(orderService).changeOrderStatus(anyLong(), any(), eq("buyer@test.com"));
    }

    @Test
    @DisplayName("processPayment: 동일 멱등성 키로 이미 완료된 결제가 있으면 기존 결제를 그대로 반환한다")
    void processPayment_멱등성_완료된결제_재반환() {
        Payment completedPayment = Payment.builder()
                .order(order)
                .idempotencyKey("IDEM-KEY-001")
                .amount(BigDecimal.valueOf(30_000))
                .paymentMethod("CARD")
                .build();
        completedPayment.complete();

        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001"))
                .thenReturn(Optional.of(completedPayment));

        PaymentResponse response = paymentService.processPayment(paymentRequest, "buyer@test.com");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        // 새로운 결제 저장이 일어나면 안 된다
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(orderRepository, orderService);
    }

    @Test
    @DisplayName("processPayment: 동일 멱등성 키로 처리 중(PENDING) 결제가 있으면 DuplicateException 발생")
    void processPayment_멱등성_처리중결제_DuplicateException() {
        Payment pendingPayment = Payment.builder()
                .order(order)
                .idempotencyKey("IDEM-KEY-001")
                .amount(BigDecimal.valueOf(30_000))
                .paymentMethod("CARD")
                .build();
        // 상태가 PENDING (기본값)

        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001"))
                .thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.processPayment(paymentRequest, "buyer@test.com"))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("processPayment: 주문을 찾을 수 없으면 NotFoundException 발생")
    void processPayment_주문없음_NotFoundException() {
        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001")).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(paymentRequest, "buyer@test.com"))
                .isInstanceOf(NotFoundException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment: 주문이 PENDING_PAYMENT 상태가 아니면 BusinessException 발생")
    void processPayment_주문상태불일치_BusinessException() {
        // 이미 PAID 상태인 주문
        order.changeStatus(OrderStatus.PAID);

        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001")).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(paymentRequest, "buyer@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("결제 대기 상태");

        verify(paymentRepository, never()).save(any());
    }

    // ═══════════════════════ cancelPayment ═══════════════════════

    @Test
    @DisplayName("cancelPayment: COMPLETED 결제를 취소하면 CANCELLED 상태가 된다")
    void cancelPayment_정상취소() {
        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey("IDEM-KEY-001")
                .amount(BigDecimal.valueOf(30_000))
                .paymentMethod("CARD")
                .build();
        payment.complete();

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.cancelPayment(1L, "admin");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelPayment: 완료되지 않은 결제는 취소할 수 없어서 BusinessException 발생")
    void cancelPayment_미완료결제_BusinessException() {
        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey("IDEM-KEY-001")
                .amount(BigDecimal.valueOf(30_000))
                .paymentMethod("CARD")
                .build();
        // status = PENDING (기본값 - 아직 완료되지 않음)

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("완료된 결제만");
    }

    @Test
    @DisplayName("cancelPayment: 결제 정보를 찾을 수 없으면 NotFoundException 발생")
    void cancelPayment_결제없음_NotFoundException() {
        when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(99L, "admin"))
                .isInstanceOf(NotFoundException.class);
    }

    // ═══════════════════════ getPaymentByOrderId ═══════════════════════

    @Test
    @DisplayName("getPaymentByOrderId: 정상적으로 결제 정보를 조회한다")
    void getPaymentByOrderId_정상조회() {
        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey("IDEM-KEY-001")
                .amount(BigDecimal.valueOf(30_000))
                .paymentMethod("CARD")
                .build();
        payment.complete();

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(1L);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
    }

    @Test
    @DisplayName("getPaymentByOrderId: 결제가 없으면 NotFoundException 발생")
    void getPaymentByOrderId_없음_NotFoundException() {
        when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
