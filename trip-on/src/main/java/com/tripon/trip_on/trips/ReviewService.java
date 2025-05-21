package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.Review;
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
    void saveReview(Long tripId, String content, List<String> filePaths);
}
