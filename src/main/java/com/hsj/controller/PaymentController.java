package com.hsj.controller;

import com.hsj.dto.common.ApiResponse;
import com.hsj.dto.payment.PaymentRequest;
import com.hsj.dto.payment.PaymentResponse;
import com.hsj.security.CustomUserDetails;
import com.hsj.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaymentResponse response = paymentService.processPayment(request, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("결제가 완료되었습니다.", response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentByOrderId(orderId)));
    }

    @PostMapping("/order/{orderId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaymentResponse response = paymentService.cancelPayment(orderId, userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("결제가 취소되었습니다.", response));
    }
}
