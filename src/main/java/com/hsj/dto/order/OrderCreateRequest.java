package com.hsj.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
    private List<OrderItemRequest> items;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {
        private Long productId;
        private int quantity;
    }
}
