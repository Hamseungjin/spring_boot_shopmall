package com.hsj.dto.product;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {

    private String name;
    private String description;

    @Positive(message = "가격은 양수여야 합니다.")
    private BigDecimal price;

    private String imageUrl;
    private Long categoryId;
}
