package com.tripon.trip_on.trips;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewLikeServiceImpl implements ReviewLikeService {

    private final ReviewLikeRepository likeRepo;

    @Override
    @Transactional
    public void like(Long reviewId, Long userId) {
        // 이미 좋아요가 없을 때만 새로 추가
        if (!likeRepo.existsByReviewIdAndUserId(reviewId, userId)) {
            ReviewLike like = new ReviewLike();
            like.setReviewId(reviewId);
            like.setUserId(userId);
            likeRepo.save(like);
        }
    }

    @Override
    @Transactional
    public void unlike(Long reviewId, Long userId) {
        likeRepo.deleteByReviewIdAndUserId(reviewId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(Long reviewId) {
        return likeRepo.countByReviewId(reviewId);
    }
}
