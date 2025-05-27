package com.tripon.trip_on.trips;

import java.util.List;

/**
 * 후기 관련 비즈니스 로직 인터페이스
 */
public interface ReviewService {

    

    /**
     * 특정 여행 계획 정보 조회
     */
    Object getTripPlan(Long tripId);

    /**
     * 특정 여행에 대한 후기 목록 조회
     */
    List<Review> getReviews(Long tripId);

    /**
     * 후기 저장
     */
    void saveReview(Long tripId, Long userId, String content, List<String> filePaths);
    void updateReview(Long reviewId, Long userId, String newContent);
    void deleteReview(Long reviewId, Long userId);
    void likeReview(Long reviewId, Long userId);
    void unlikeReview(Long reviewId, Long userId);
}
