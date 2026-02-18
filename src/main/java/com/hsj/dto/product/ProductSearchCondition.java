package com.hsj.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductSearchCondition {

    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;
    private String sortBy;
    private String sortDirection;

    @Builder
    public ProductSearchCondition(String keyword, Long categoryId, BigDecimal minPrice,
                                  BigDecimal maxPrice, Boolean inStock,
                                  String sortBy, String sortDirection) {
        this.keyword = keyword;
        this.categoryId = categoryId;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.inStock = inStock;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }
}
