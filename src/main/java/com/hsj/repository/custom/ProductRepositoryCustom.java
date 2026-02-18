package com.hsj.repository.custom;

import com.hsj.dto.product.ProductSearchCondition;
import com.hsj.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable);
}
