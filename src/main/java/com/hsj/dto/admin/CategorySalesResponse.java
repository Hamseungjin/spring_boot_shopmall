package com.hsj.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CategorySalesResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal revenue;
    private long quantity;
}
