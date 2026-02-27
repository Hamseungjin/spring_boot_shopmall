package com.hsj.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotBlank(message = "배송지 주소는 필수입니다.")
    private String shippingAddress;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String receiverName;

    @NotBlank(message = "수령인 전화번호는 필수입니다.")
    private String receiverPhone;

    @NotEmpty(message = "주문 상품이 비어있습니다.")
    @Valid
    private List<OrderItemRequest> items;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @Positive(message = "수량은 1 이상이어야 합니다.")
        @Max(value = 100, message = "수량은 최대 100개까지 가능합니다.")
        private int quantity;
    }
}
