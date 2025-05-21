package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.Review;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * ReviewService 구현체
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    @Override
    public Object getTripPlan(Long tripId) {
        // TODO: 실제 여행 계획 데이터를 조회하는 로직 구현
        return null;
    }

    @Override
    public List<Review> getReviews(Long tripId) {
        // TODO: 후기 리스트를 조회하는 로직 구현
        return List.of();
    }

    @Override
    public void saveReview(Long tripId, String content, List<String> filePaths) {
        // TODO: 후기 저장 로직 구현
    }
}

