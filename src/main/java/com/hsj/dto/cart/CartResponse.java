package com.hsj.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {

    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;

    @Getter
    @Builder
    public static class CartItemResponse {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }
}
