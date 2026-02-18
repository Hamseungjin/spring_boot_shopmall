package com.hsj.dto.order;

import com.hsj.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangeRequest {

    @NotNull(message = "변경할 상태는 필수입니다.")
    private OrderStatus status;

    private String reason;

    public static OrderStatusChangeRequest of(OrderStatus status, String reason) {
        return new OrderStatusChangeRequest(status, reason);
    }
}
