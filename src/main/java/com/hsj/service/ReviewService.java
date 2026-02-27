package com.hsj.service;

import com.hsj.dto.common.PageResponse;
import com.hsj.dto.review.ReviewCreateRequest;
import com.hsj.dto.review.ReviewResponse;
import com.hsj.dto.review.ReviewUpdateRequest;
import com.hsj.entity.Member;
import com.hsj.entity.Order;
import com.hsj.entity.Product;
import com.hsj.entity.Review;
import com.hsj.entity.enums.OrderStatus;
import com.hsj.exception.BusinessException;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.MemberRepository;
import com.hsj.repository.OrderRepository;
import com.hsj.repository.ProductRepository;
import com.hsj.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    /**
     * 리뷰 작성
     * - 배송 완료(DELIVERED) 상태의 주문에만 작성 가능
     * - 해당 주문에 포함된 상품만 리뷰 가능
     * - 동일 주문·상품 조합에 대해 중복 리뷰 불가
     */
    @Transactional
    public ReviewResponse createReview(Long memberId, ReviewCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        Order order = orderRepository.findByIdAndDeletedFalse(request.getOrderId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 주문에 대해서만 리뷰를 작성할 수 있습니다.");
        }

        // 배송 완료 상태인지 확인
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_DELIVERED);
        }

        // 주문에 해당 상품이 포함되어 있는지 확인
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));
        if (!productInOrder) {
            throw new BusinessException(ErrorCode.ORDER_PRODUCT_MISMATCH);
        }

        // 중복 리뷰 확인
        if (reviewRepository.existsByMemberIdAndOrderIdAndProductId(
                memberId, request.getOrderId(), request.getProductId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        Review review = Review.builder()
                .member(member)
                .product(product)
                .order(order)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("리뷰 작성 완료: reviewId={}, memberId={}, productId={}", saved.getId(), memberId, request.getProductId());

        return ReviewResponse.from(saved);
    }

    /**
     * 상품별 리뷰 목록 조회 (공개 — 비로그인도 가능)
     */
    public PageResponse<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Page<ReviewResponse> page = reviewRepository.findByProductIdAndDeletedFalse(productId, pageable)
                .map(ReviewResponse::from);
        return PageResponse.from(page);
    }

    /**
     * 리뷰 수정 — 본인이 작성한 리뷰만 수정 가능
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long memberId, ReviewUpdateRequest request) {
        Review review = findReviewOrThrow(reviewId);
        verifyReviewOwnership(review, memberId);

        review.update(request.getRating(), request.getContent());
        log.info("리뷰 수정 완료: reviewId={}, memberId={}", reviewId, memberId);

        return ReviewResponse.from(review);
    }

    /**
     * 리뷰 삭제 (soft delete) — 본인 리뷰 또는 관리자만 삭제 가능
     */
    @Transactional
    public void deleteReview(Long reviewId, Long memberId, boolean isAdmin) {
        Review review = findReviewOrThrow(reviewId);

        if (!isAdmin) {
            verifyReviewOwnership(review, memberId);
        }

        review.softDelete();
        log.info("리뷰 삭제 완료: reviewId={}, deletedBy={}", reviewId, memberId);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Review findReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void verifyReviewOwnership(Review review, Long memberId) {
        if (!review.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 작성한 리뷰만 수정/삭제할 수 있습니다.");
        }
    }
}
