package com.hsj.repository;

import com.hsj.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Order> findByIdAndDeletedFalse(Long id);

    /**
     * 페이지네이션 쿼리와 orderItems JOIN FETCH를 분리해 Hibernate의
     * "in-memory pagination" 경고를 방지한다.
     * countQuery는 JOIN 없이 실행돼 정확한 전체 건수를 보장한다.
     */
    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.member.id = :memberId AND o.deleted = false",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o WHERE o.member.id = :memberId AND o.deleted = false")
    Page<Order> findByMemberIdAndDeletedFalse(@Param("memberId") Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Order> findByOrderNumberAndDeletedFalse(String orderNumber);
}
