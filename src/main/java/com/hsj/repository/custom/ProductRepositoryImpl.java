package com.hsj.repository.custom;

import com.hsj.dto.product.ProductSearchCondition;
import com.hsj.entity.Product;
import com.hsj.entity.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable) {
        QProduct product = QProduct.product;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(product.deleted.isFalse());

        if (StringUtils.hasText(condition.getKeyword())) {
            builder.and(
                    product.name.containsIgnoreCase(condition.getKeyword())
                            .or(product.description.containsIgnoreCase(condition.getKeyword()))
            );
        }

        if (condition.getCategoryId() != null) {
            builder.and(product.category.id.eq(condition.getCategoryId()));
        }

        if (condition.getMinPrice() != null) {
            builder.and(product.price.goe(condition.getMinPrice()));
        }

        if (condition.getMaxPrice() != null) {
            builder.and(product.price.loe(condition.getMaxPrice()));
        }

        if (condition.getInStock() != null && condition.getInStock()) {
            builder.and(product.stockQuantity.gt(0));
        }

        OrderSpecifier<?> orderSpecifier = resolveOrder(product, condition);

        List<Product> content = queryFactory
                .selectFrom(product)
                .leftJoin(product.category).fetchJoin()
                .where(builder)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private OrderSpecifier<?> resolveOrder(QProduct product, ProductSearchCondition condition) {
        String sortBy = condition.getSortBy();
        boolean asc = "asc".equalsIgnoreCase(condition.getSortDirection());

        if (!StringUtils.hasText(sortBy)) {
            return product.createdAt.desc();
        }

        return switch (sortBy) {
            case "price" -> asc ? product.price.asc() : product.price.desc();
            case "name" -> asc ? product.name.asc() : product.name.desc();
            case "stock" -> asc ? product.stockQuantity.asc() : product.stockQuantity.desc();
            default -> product.createdAt.desc();
        };
    }
}
