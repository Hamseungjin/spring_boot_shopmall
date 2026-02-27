package com.hsj.controller;

import com.hsj.dto.common.ApiResponse;
import com.hsj.dto.common.PageResponse;
import com.hsj.dto.review.ReviewCreateRequest;
import com.hsj.dto.review.ReviewResponse;
import com.hsj.dto.review.ReviewUpdateRequest;
import com.hsj.security.CustomUserDetails;
import com.hsj.service.ReviewService;
import com.hsj.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성
     * POST /api/reviews
     * - 인증 필요, 배송 완료 주문의 상품에 대해서만 작성 가능
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(userDetails.getMemberId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("리뷰가 작성되었습니다.", response));
    }

    /**
     * 상품별 리뷰 목록 조회 (공개)
     * GET /api/reviews/products/{productId}?page=0&size=10
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageUtils.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.ok(reviewService.getReviewsByProduct(productId, pageable)));
    }

    /**
     * 리뷰 수정
     * PATCH /api/reviews/{reviewId}
     * - 본인이 작성한 리뷰만 수정 가능
     */
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewUpdateRequest request) {
        ReviewResponse response = reviewService.updateReview(
                reviewId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.ok("리뷰가 수정되었습니다.", response));
    }

    /**
     * 리뷰 삭제 (soft delete)
     * DELETE /api/reviews/{reviewId}
     * - 본인 리뷰 또는 관리자만 삭제 가능
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.deleteReview(reviewId, userDetails.getMemberId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("리뷰가 삭제되었습니다.", null));
    }
}
