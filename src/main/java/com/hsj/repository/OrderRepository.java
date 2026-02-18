package com.hsj.repository;

import com.hsj.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndDeletedFalse(Long id);

    Page<Order> findByMemberIdAndDeletedFalse(Long memberId, Pageable pageable);

    Optional<Order> findByOrderNumberAndDeletedFalse(String orderNumber);
}
