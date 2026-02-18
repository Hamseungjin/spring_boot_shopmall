package com.hsj.repository.custom;

import com.hsj.dto.admin.CategorySalesResponse;
import com.hsj.dto.admin.DailySalesResponse;
import com.hsj.entity.QMember;
import com.hsj.entity.QOrder;
import com.hsj.entity.QOrderItem;
import com.hsj.entity.QPayment;
import com.hsj.entity.enums.OrderItemStatus;
import com.hsj.entity.enums.PaymentStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminStatsRepository {

    private final JPAQueryFactory queryFactory;

    public BigDecimal getTotalRevenue(LocalDateTime from, LocalDateTime to) {
        QPayment payment = QPayment.payment;

        BigDecimal result = queryFactory
                .select(payment.amount.sum())
                .from(payment)
                .where(
                        payment.status.eq(PaymentStatus.COMPLETED),
                        payment.createdAt.between(from, to)
                )
                .fetchOne();

        return result != null ? result : BigDecimal.ZERO;
    }

    public long getTotalOrderCount(LocalDateTime from, LocalDateTime to) {
        QOrder order = QOrder.order;

        Long count = queryFactory
                .select(order.count())
                .from(order)
                .where(
                        order.deleted.isFalse(),
                        order.createdAt.between(from, to)
                )
                .fetchOne();

        return count != null ? count : 0;
    }

    public long getPaidOrderCount(LocalDateTime from, LocalDateTime to) {
        QPayment payment = QPayment.payment;

        Long count = queryFactory
                .select(payment.count())
                .from(payment)
                .where(
                        payment.status.eq(PaymentStatus.COMPLETED),
                        payment.createdAt.between(from, to)
                )
                .fetchOne();

        return count != null ? count : 0;
    }

    public long getNewMemberCount(LocalDateTime from, LocalDateTime to) {
        QMember member = QMember.member;

        Long count = queryFactory
                .select(member.count())
                .from(member)
                .where(
                        member.deleted.isFalse(),
                        member.createdAt.between(from, to)
                )
                .fetchOne();

        return count != null ? count : 0;
    }

    public List<DailySalesResponse> getDailySales(LocalDate from, LocalDate to) {
        QPayment payment = QPayment.payment;

        DateTemplate<LocalDate> dateExpr = Expressions.dateTemplate(
                LocalDate.class, "CAST({0} AS date)", payment.createdAt);

        List<Tuple> results = queryFactory
                .select(dateExpr, payment.amount.sum(), payment.count())
                .from(payment)
                .where(
                        payment.status.eq(PaymentStatus.COMPLETED),
                        payment.createdAt.between(
                                from.atStartOfDay(),
                                to.atTime(LocalTime.MAX)
                        )
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();

        return results.stream()
                .map(tuple -> new DailySalesResponse(
                        tuple.get(dateExpr),
                        tuple.get(payment.amount.sum()) != null
                                ? tuple.get(payment.amount.sum()) : BigDecimal.ZERO,
                        tuple.get(payment.count()) != null
                                ? tuple.get(payment.count()) : 0
                ))
                .toList();
    }

    public List<CategorySalesResponse> getCategorySales(LocalDateTime from, LocalDateTime to) {
        QOrderItem orderItem = QOrderItem.orderItem;

        List<Tuple> results = queryFactory
                .select(
                        orderItem.product.category.id,
                        orderItem.product.category.name,
                        orderItem.snapshotPrice.multiply(orderItem.quantity).sum(),
                        orderItem.quantity.sum()
                )
                .from(orderItem)
                .where(
                        orderItem.status.ne(OrderItemStatus.CANCELLED),
                        orderItem.product.category.isNotNull(),
                        orderItem.createdAt.between(from, to)
                )
                .groupBy(orderItem.product.category.id, orderItem.product.category.name)
                .orderBy(orderItem.snapshotPrice.multiply(orderItem.quantity).sum().desc())
                .fetch();

        return results.stream()
                .map(tuple -> new CategorySalesResponse(
                        tuple.get(orderItem.product.category.id),
                        tuple.get(orderItem.product.category.name),
                        tuple.get(orderItem.snapshotPrice.multiply(orderItem.quantity).sum()) != null
                                ? tuple.get(orderItem.snapshotPrice.multiply(orderItem.quantity).sum())
                                : BigDecimal.ZERO,
                        tuple.get(orderItem.quantity.sum()) != null
                                ? tuple.get(orderItem.quantity.sum()) : 0
                ))
                .toList();
    }

    public long getTotalVisitorCount(LocalDateTime from, LocalDateTime to) {
        com.hsj.entity.QEventLog eventLog = com.hsj.entity.QEventLog.eventLog;

        Long count = queryFactory
                .select(eventLog.sessionId.countDistinct())
                .from(eventLog)
                .where(eventLog.createdAt.between(from, to))
                .fetchOne();

        return count != null ? count : 0;
    }
}
