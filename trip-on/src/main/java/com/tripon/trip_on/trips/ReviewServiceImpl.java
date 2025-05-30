package com.tripon.trip_on.trips;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.tripon.trip_on.plan.TripRepository;
import jakarta.persistence.EntityNotFoundException;



/**
 * ReviewService 구현체
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TripRepository tripRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, TripRepository tripRepository, ReviewLikeRepository reviewLikeRepository) {
        this.reviewRepository = reviewRepository;
        this.tripRepository = tripRepository;
        this.reviewLikeRepository = reviewLikeRepository;
    }

    @Override
    public Object getTripPlan(Long tripId) {
        // Trip 엔티티에서 tripId로 여행 정보 조회
        return tripRepository.findById(tripId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviews(Long tripId) {
        List<Review> reviews = reviewRepository.findByTripId(tripId);
        // 좋아요 수를 각 엔티티에 세팅
        for (Review r : reviews) {
            long count = reviewLikeRepository.countByReviewId(r.getId());
            r.setLikeCount(count);
        }
        return reviews;
    }

    @Override
    public void saveReview(Long tripId, Long userId, String content, List<String> filePaths) {
        Review review = Review.builder()
                .tripId(tripId)
                .userId(userId)
                .content(content)
                .filePaths(filePaths)
                .build();
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void updateReview(Long reviewId, Long userId, String newContent) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("후기를 찾을 수 없습니다."));
        if (!r.getUserId().equals(userId)) {
            throw new RuntimeException("본인의 후기만 수정할 수 있습니다.");
        }
        r.setContent(newContent);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("후기를 찾을 수 없습니다."));
        if (!r.getUserId().equals(userId)) {
            throw new RuntimeException("본인의 후기만 삭제할 수 있습니다.");
        }
        reviewRepository.delete(r);
    }

    // (좋아요 / 좋아요 취소 로직은, review_like 테이블을 관리하는 Repository를 이용해 구현)
    @Override
    public void likeReview(Long reviewId, Long userId) {
        // TODO: 좋아요 기능 구현
    }

    @Override
    @Transactional
    public void unlikeReview(Long reviewId, Long userId) {
        // review_like 테이블에서 해당 reviewId, userId 조합을 삭제
        reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
    }


}

