package com.hsj.repository;

import com.hsj.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndDeletedFalse(Long productId, Pageable pageable);

    boolean existsByMemberIdAndOrderIdAndProductId(Long memberId, Long orderId, Long productId);
}
