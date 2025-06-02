package com.tripon.trip_on.trips;

import org.springframework.stereotype.Service; // 서비스 빈 등록
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리
import lombok.RequiredArgsConstructor; // 생성자 자동 생성

@Service // 서비스 빈 등록
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class ReviewLikeServiceImpl implements ReviewLikeService {

    private final ReviewLikeRepository likeRepo; // 좋아요 레포지토리

    @Override
    @Transactional
    public void like(Long reviewId, Long userId) {
        // 이미 좋아요가 없을 때만 새로 추가
        if (!likeRepo.existsByReviewIdAndUserId(reviewId, userId)) {
            ReviewLike like = new ReviewLike();
            like.setReviewId(reviewId);
            like.setUserId(userId);
            likeRepo.save(like); // 좋아요 저장
        }
    }

    @Override
    @Transactional
    public void unlike(Long reviewId, Long userId) {
        likeRepo.deleteByReviewIdAndUserId(reviewId, userId); // 좋아요 삭제
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(Long reviewId) {
        return likeRepo.countByReviewId(reviewId); // 좋아요 개수 반환
    }
}
