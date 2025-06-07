package com.tripon.trip_on.trips;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.tripon.trip_on.trips.TripRepository;
import com.tripon.trip_on.trips.Trip;
import jakarta.persistence.EntityNotFoundException;
import com.tripon.trip_on.service.S3Service;

/**
 * ReviewService 구현체 (비즈니스 로직)
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TripRepository tripRepository; // TripRepository 주입
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final S3Service s3Service;

    public ReviewServiceImpl(ReviewRepository reviewRepository, TripRepository tripRepository, ReviewLikeRepository reviewLikeRepository, ReviewPhotoRepository reviewPhotoRepository, S3Service s3Service) {
        this.reviewRepository = reviewRepository;
        this.tripRepository = tripRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewPhotoRepository = reviewPhotoRepository;
        this.s3Service = s3Service;
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
        // 각 후기별로 좋아요 수와 좋아요 상태 세팅
        for (Review r : reviews) {
            long count = reviewLikeRepository.countByReviewId(r.getId());
            r.setLikeCount(count);
            
            // 현재 사용자가 좋아요를 눌렀는지 확인
            boolean liked = reviewLikeRepository.existsByReviewIdAndUserId(r.getId(), getCurrentUserId());
            r.setLiked(liked);
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
        // 사진 삭제는 별도의 API에서 처리하므로 여기서는 삭제하지 않음!!
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

    @Override
    @Transactional
    public void saveReviewPhoto(Long reviewId, String imageUrl, String filePath, String fileType) {
        try {
            // 리뷰 존재 여부 확인
            if (!reviewRepository.existsById(reviewId)) {
                throw new EntityNotFoundException("리뷰를 찾을 수 없습니다.");
            }

            ReviewPhoto photo = ReviewPhoto.builder()
                    .reviewId(reviewId)
                    .imageUrl(imageUrl)
                    .filePath(filePath)
                    .fileType(fileType)
                    .build();
            
            reviewPhotoRepository.save(photo);
        } catch (Exception e) {
            System.err.println("ReviewPhoto save failed: " + e.getMessage());
            throw e;
        }
    }

    // 현재 로그인한 사용자 ID를 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        // TODO: SecurityContextHolder나 Principal에서 현재 사용자 ID를 가져오도록 구현
        return 0L; // 임시로 0 반환
    }

    @Override
    @Transactional
    public Review saveReviewAndReturn(Long tripId, Long userId, String content) {
        try {
            // 여행 존재 여부 확인
            if (!tripRepository.existsById(tripId)) {
                throw new EntityNotFoundException("여행을 찾을 수 없습니다.");
            }

            Review review = Review.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .content(content)
                    .build();
            
            return reviewRepository.save(review);
        } catch (Exception e) {
            System.err.println("Review save failed: " + e.getMessage());
            throw e;
        }
    }

}

