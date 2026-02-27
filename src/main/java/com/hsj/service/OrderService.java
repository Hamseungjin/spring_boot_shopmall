package com.hsj.service;

import com.hsj.dto.common.PageResponse;
import com.hsj.dto.order.*;
import com.hsj.entity.*;
import com.hsj.entity.enums.OrderItemStatus;
import com.hsj.entity.enums.OrderStatus;
import com.hsj.entity.enums.PaymentStatus;
import com.hsj.exception.BusinessException;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final StockService stockService;

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER_ITEMS);
        }

        Order order = Order.builder()
                .member(member)
                .shippingAddress(request.getShippingAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .build();

        List<StockDeduction> deducted = new ArrayList<>();

        try {
            for (OrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
                stockService.deductStock(itemReq.getProductId(), itemReq.getQuantity());
                deducted.add(new StockDeduction(itemReq.getProductId(), itemReq.getQuantity()));

                Product product = productRepository.findByIdAndDeletedFalse(itemReq.getProductId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

                OrderItem orderItem = OrderItem.builder()
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .build();

                order.addOrderItem(orderItem);
            }
        } catch (Exception e) {
            rollbackStock(deducted);
            throw e;
        }

        Order saved = orderRepository.save(order);

        orderHistoryRepository.save(
                OrderHistory.record(saved, null, OrderStatus.PENDING_PAYMENT,
                        "주문 생성", member.getEmail())
        );

        log.info("주문 생성 완료: orderId={}, orderNumber={}, memberId={}, totalAmount={}",
                saved.getId(), saved.getOrderNumber(), memberId, saved.getTotalAmount());

        return OrderResponse.from(saved);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        return OrderResponse.from(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndDeletedFalse(orderNumber)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    public PageResponse<OrderResponse> getMyOrders(Long memberId, Pageable pageable) {
        Page<OrderResponse> page = orderRepository.findByMemberIdAndDeletedFalse(memberId, pageable)
                .map(OrderResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public OrderResponse changeOrderStatus(Long orderId, OrderStatusChangeRequest request, String changedBy) {
        Order order = findOrderOrThrow(orderId);
        OrderStatus previousStatus = order.getStatus();

        order.changeStatus(request.getStatus());

        if (request.getStatus() == OrderStatus.PAID) {
            order.getOrderItems().forEach(item -> item.changeStatus(OrderItemStatus.PAID));
        } else if (request.getStatus() == OrderStatus.PREPARING) {
            order.getOrderItems().forEach(item -> {
                if (item.getStatus() == OrderItemStatus.PAID) {
                    item.changeStatus(OrderItemStatus.PREPARING);
                }
            });
        } else if (request.getStatus() == OrderStatus.SHIPPED) {
            order.getOrderItems().forEach(item -> {
                if (item.getStatus() == OrderItemStatus.PREPARING) {
                    item.changeStatus(OrderItemStatus.SHIPPED);
                }
            });
        } else if (request.getStatus() == OrderStatus.DELIVERED) {
            order.getOrderItems().forEach(item -> {
                if (item.getStatus() == OrderItemStatus.SHIPPED) {
                    item.changeStatus(OrderItemStatus.DELIVERED);
                }
            });
        }

        orderHistoryRepository.save(
                OrderHistory.record(order, previousStatus, request.getStatus(),
                        request.getReason(), changedBy)
        );

        log.info("주문 상태 변경: orderId={}, {} → {}", orderId, previousStatus, request.getStatus());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String reason, String cancelledBy) {
        Order order = findOrderOrThrow(orderId);

        if (!order.isCancellable()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }

        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getOrderItems()) {
            if (item.isCancellable()) {
                item.changeStatus(OrderItemStatus.CANCELLED);
                stockService.restoreStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        // 결제가 완료된 상태에서 취소하는 경우 결제도 취소 처리
        if (previousStatus == OrderStatus.PAID || previousStatus == OrderStatus.PREPARING) {
            paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    payment.cancel();
                    log.info("결제 취소 처리: orderId={}, paymentId={}", orderId, payment.getId());
                }
            });
        }

        orderHistoryRepository.save(
                OrderHistory.record(order, previousStatus, OrderStatus.CANCELLED,
                        reason != null ? reason : "고객 요청 취소", cancelledBy)
        );

        log.info("주문 취소 완료: orderId={}, orderNumber={}", orderId, order.getOrderNumber());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrderItem(Long orderId, Long orderItemId, String reason, String cancelledBy) {
        Order order = findOrderOrThrow(orderId);

        OrderItem target = order.getOrderItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (!target.isCancellable()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "취소할 수 없는 주문 아이템 상태입니다: " + target.getStatus());
        }

        target.changeStatus(OrderItemStatus.CANCELLED);
        stockService.restoreStock(target.getProduct().getId(), target.getQuantity());

        order.calculateTotalAmount();

        orderHistoryRepository.save(
                OrderHistory.record(order, order.getStatus(), order.getStatus(),
                        "아이템 부분 취소: " + target.getSnapshotProductName() +
                                (reason != null ? " - " + reason : ""),
                        cancelledBy)
        );

        log.info("주문 아이템 취소: orderId={}, orderItemId={}", orderId, orderItemId);
        return OrderResponse.from(order);
    }

    public List<OrderHistoryResponse> getOrderHistory(Long orderId) {
        findOrderOrThrow(orderId);
        return orderHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(OrderHistoryResponse::from)
                .toList();
    }

    /** 요청자가 주문 소유자인지 확인한다. 아닐 경우 ACCESS_DENIED 예외를 던진다. */
    public void verifyOwnership(Long orderId, Long memberId) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 주문만 접근할 수 있습니다.");
        }
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void rollbackStock(List<StockDeduction> deducted) {
        for (StockDeduction d : deducted) {
            try {
                stockService.restoreStock(d.productId(), d.quantity());
            } catch (Exception rollbackEx) {
                log.error("재고 복원 실패: productId={}, quantity={}", d.productId(), d.quantity(), rollbackEx);
            }
        }
    }

    private record StockDeduction(Long productId, int quantity) {
    }
}
