package com.hsj.controller;

import com.hsj.dto.cart.CartAddRequest;
import com.hsj.dto.cart.CartResponse;
import com.hsj.dto.common.ApiResponse;
import com.hsj.security.CustomUserDetails;
import com.hsj.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userDetails.getMemberId())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartAddRequest request) {
        CartResponse response = cartService.addItem(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.ok("장바구니에 추가되었습니다.", response));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        CartResponse response = cartService.updateItemQuantity(
                userDetails.getMemberId(), productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {
        CartResponse response = cartService.removeItem(userDetails.getMemberId(), productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok("장바구니가 비워졌습니다."));
    }
}
