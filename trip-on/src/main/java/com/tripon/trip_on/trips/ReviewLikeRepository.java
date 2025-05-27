package com.tripon.trip_on.trips;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, ReviewLikeId> {
        // 특정 리뷰에 대해 사용자가 이미 좋아요를 눌렀는지 체크
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

        // 좋아요 삭제
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);
    
        // (선택) 좋아요 개수 조회용
    long countByReviewId(Long reviewId);
    

}