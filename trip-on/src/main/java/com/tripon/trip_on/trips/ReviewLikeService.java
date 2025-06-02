package com.tripon.trip_on.trips;

// 리뷰 좋아요 관련 비즈니스 로직 인터페이스
public interface ReviewLikeService {
    void like(Long reviewId, Long userId); // 좋아요 등록
    void unlike(Long reviewId, Long userId); // 좋아요 취소
    long getLikeCount(Long reviewId); // 좋아요 개수 조회
}
