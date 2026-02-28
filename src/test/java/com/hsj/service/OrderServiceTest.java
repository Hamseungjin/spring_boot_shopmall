package com.hsj.service;

import com.hsj.dto.order.OrderCreateRequest;
import com.hsj.dto.order.OrderResponse;
import com.hsj.dto.order.OrderStatusChangeRequest;
import com.hsj.entity.Member;
import com.hsj.entity.Order;
import com.hsj.entity.OrderHistory;
import com.hsj.entity.OrderItem;
import com.hsj.entity.Product;
import com.hsj.entity.enums.MemberRole;
import com.hsj.entity.enums.OrderStatus;
import com.hsj.exception.BusinessException;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.MemberRepository;
import com.hsj.repository.OrderHistoryRepository;
import com.hsj.repository.OrderItemRepository;
import com.hsj.repository.OrderRepository;
import com.hsj.repository.PaymentRepository;
import com.hsj.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderHistoryRepository orderHistoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private StockService stockService;

    @InjectMocks
    private OrderService orderService;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@test.com")
                .password("encoded_pw")
                .name("테스터")
                .role(MemberRole.CUSTOMER)
                .build();

        product = Product.builder()
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10_000))
                .stockQuantity(50)
                .build();
    }

    // ─── 헬퍼: OrderCreateRequest 필드를 Reflection으로 세팅 ─────────────

    private OrderCreateRequest buildRequest(Long productId, int quantity) {
        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "shippingAddress", "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(request, "receiverName", "홍길동");
        ReflectionTestUtils.setField(request, "receiverPhone", "010-1234-5678");

        OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
        ReflectionTestUtils.setField(item, "productId", productId);
        ReflectionTestUtils.setField(item, "quantity", quantity);

        ReflectionTestUtils.setField(request, "items", List.of(item));
        return request;
    }

    // ═══════════════════════ createOrder ═══════════════════════

    @Test
    @DisplayName("createOrder: 정상 주문 생성 - 재고 차감 후 Order가 저장된다")
    void createOrder_정상생성() {
        OrderCreateRequest request = buildRequest(1L, 2);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        doNothing().when(stockService).deductStock(1L, 2);

        // save()가 호출될 때 넘겨받은 Order 객체 그대로 반환
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(stockService).deductStock(1L, 2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: 회원이 없으면 NotFoundException 발생")
    void createOrder_회원없음_NotFoundException() {
        OrderCreateRequest request = buildRequest(1L, 1);
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(99L, request))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(stockService, orderRepository);
    }

    @Test
    @DisplayName("createOrder: 주문 상품이 비어있으면 BusinessException 발생")
    void createOrder_빈상품목록_BusinessException() {
        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "shippingAddress", "addr");
        ReflectionTestUtils.setField(request, "receiverName", "name");
        ReflectionTestUtils.setField(request, "receiverPhone", "phone");
        ReflectionTestUtils.setField(request, "items", List.of());

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(stockService, orderRepository);
    }

    @Test
    @DisplayName("createOrder: 재고 차감 실패 시 이미 차감된 재고를 롤백한다")
    void createOrder_재고차감실패_롤백() {
        // 상품 2개짜리 주문 (productId=1, productId=2)
        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "shippingAddress", "addr");
        ReflectionTestUtils.setField(request, "receiverName", "name");
        ReflectionTestUtils.setField(request, "receiverPhone", "phone");

        OrderCreateRequest.OrderItemRequest item1 = new OrderCreateRequest.OrderItemRequest();
        ReflectionTestUtils.setField(item1, "productId", 1L);
        ReflectionTestUtils.setField(item1, "quantity", 2);

        OrderCreateRequest.OrderItemRequest item2 = new OrderCreateRequest.OrderItemRequest();
        ReflectionTestUtils.setField(item2, "productId", 2L);
        ReflectionTestUtils.setField(item2, "quantity", 3);

        ReflectionTestUtils.setField(request, "items", List.of(item1, item2));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        doNothing().when(stockService).deductStock(1L, 2);                       // 첫 번째 차감 성공
        doThrow(new RuntimeException("재고 서버 오류")).when(stockService).deductStock(2L, 3); // 두 번째 차감 실패

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(RuntimeException.class);

        // 첫 번째 차감분 롤백 호출 확인
        verify(stockService).restoreStock(1L, 2);
        verify(orderRepository, never()).save(any());
    }

    // ═══════════════════════ getOrder ═══════════════════════

    @Test
    @DisplayName("getOrder: 존재하는 주문 ID이면 OrderResponse를 반환한다")
    void getOrder_정상조회() {
        Order order = Order.builder()
                .member(member)
                .shippingAddress("addr")
                .receiverName("name")
                .receiverPhone("phone")
                .build();
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("getOrder: 존재하지 않는 주문 ID이면 NotFoundException 발생")
    void getOrder_없는ID_NotFoundException() {
        when(orderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(NotFoundException.class);
    }

    // ═══════════════════════ cancelOrder ═══════════════════════

    @Test
    @DisplayName("cancelOrder: PENDING_PAYMENT 상태 주문을 취소하면 재고가 복원된다")
    void cancelOrder_정상취소() {
        Order order = Order.builder()
                .member(member)
                .shippingAddress("addr")
                .receiverName("name")
                .receiverPhone("phone")
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(3)
                .build();
        order.addOrderItem(item);

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        doNothing().when(stockService).restoreStock(anyLong(), anyInt());
        when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(1L, "고객 변심", "test@test.com");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(stockService).restoreStock(any(), eq(3));
    }

    @Test
    @DisplayName("cancelOrder: 취소 불가 상태(SHIPPED)이면 BusinessException 발생")
    void cancelOrder_취소불가상태_BusinessException() {
        Order order = Order.builder()
                .member(member)
                .shippingAddress("addr")
                .receiverName("name")
                .receiverPhone("phone")
                .build();
        // PENDING_PAYMENT → PAID → PREPARING → SHIPPED
        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.PREPARING);
        order.changeStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "취소 시도", "user"))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(stockService);
    }

    // ═══════════════════════ changeOrderStatus ═══════════════════════

    @Test
    @DisplayName("changeOrderStatus: PENDING_PAYMENT → PAID 상태 전이가 성공한다")
    void changeOrderStatus_PAID_전이성공() {
        Order order = Order.builder()
                .member(member)
                .shippingAddress("addr")
                .receiverName("name")
                .receiverPhone("phone")
                .build();

        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderStatusChangeRequest req = OrderStatusChangeRequest.of(OrderStatus.PAID, "결제 완료");
        OrderResponse response = orderService.changeOrderStatus(1L, req, "admin");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("changeOrderStatus: 불가능한 상태 전이면 BusinessException 발생")
    void changeOrderStatus_불가전이_BusinessException() {
        Order order = Order.builder()
                .member(member)
                .shippingAddress("addr")
                .receiverName("name")
                .receiverPhone("phone")
                .build();
        // PENDING_PAYMENT → DELIVERED 는 직접 전이 불가
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

        OrderStatusChangeRequest req = OrderStatusChangeRequest.of(OrderStatus.DELIVERED, "강제 변경");

        assertThatThrownBy(() -> orderService.changeOrderStatus(1L, req, "admin"))
                .isInstanceOf(Exception.class); // IllegalStateException (OrderStatus.transitionTo)
    }
}
