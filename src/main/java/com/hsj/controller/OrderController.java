package com.hsj.controller;

import com.hsj.dto.common.ApiResponse;
import com.hsj.dto.common.PageResponse;
import com.hsj.dto.order.*;
import com.hsj.security.CustomUserDetails;
import com.hsj.service.OrderService;
import com.hsj.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(userDetails.getMemberId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("주문이 생성되었습니다.", response));
    }

    /**
     * 단건 주문 조회 — ADMIN은 모든 주문 조회 가능, CUSTOMER는 본인 주문만 조회 가능.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            orderService.verifyOwnership(orderId, userDetails.getMemberId());
        }
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrder(orderId)));
    }

    /**
     * 주문번호로 주문 조회 — ADMIN은 모든 주문 조회 가능, CUSTOMER는 본인 주문만 조회 가능.
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        if (!isAdmin(userDetails) && !response.getMemberId().equals(userDetails.getMemberId())) {
            orderService.verifyOwnership(response.getOrderId(), userDetails.getMemberId());
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageUtils.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.ok(orderService.getMyOrders(userDetails.getMemberId(), pageable)));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> changeStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderResponse response = orderService.changeOrderStatus(
                orderId, request, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("주문 상태가 변경되었습니다.", response));
    }

    /**
     * 주문 전체 취소 — 본인 주문만 취소 가능.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderCancelRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.verifyOwnership(orderId, userDetails.getMemberId());
        String reason = request != null ? request.getReason() : null;
        OrderResponse response = orderService.cancelOrder(
                orderId, reason, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("주문이 취소되었습니다.", response));
    }

    /**
     * 주문 아이템 부분 취소 — 본인 주문만 취소 가능.
     */
    @PostMapping("/{orderId}/items/{orderItemId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestBody(required = false) OrderCancelRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.verifyOwnership(orderId, userDetails.getMemberId());
        String reason = request != null ? request.getReason() : null;
        OrderResponse response = orderService.cancelOrderItem(
                orderId, orderItemId, reason, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("주문 아이템이 취소되었습니다.", response));
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<ApiResponse<List<OrderHistoryResponse>>> getOrderHistory(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            orderService.verifyOwnership(orderId, userDetails.getMemberId());
        }
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderHistory(orderId)));
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private boolean isAdmin(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
