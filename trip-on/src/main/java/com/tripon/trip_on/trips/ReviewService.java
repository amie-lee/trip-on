package com.tripon.trip_on.trips;

import java.util.List;

/**
 * 후기 관련 비즈니스 로직 인터페이스
 */
public interface ReviewService {

    /**
     * 특정 여행 계획 정보 조회
     * @param tripId 여행 ID
     * @return 여행 정보 객체
     */
    Object getTripPlan(Long tripId);

    /**
     * 특정 여행에 대한 후기 목록 조회
     * @param tripId 여행 ID
     * @return 후기 리스트
     */
    List<Review> getReviews(Long tripId);

    /**
     * 후기 저장
     * @param tripId 여행 ID
     * @param userId 작성자 ID
     * @param content 후기 내용
     * @param filePaths 첨부파일 경로 리스트
     */
    void saveReview(Long tripId, Long userId, String content, List<String> filePaths);

    /**
     * 후기 수정
     * @param reviewId 후기 ID
     * @param userId 작성자 ID
     * @param newContent 수정할 내용
     */
    void updateReview(Long reviewId, Long userId, String newContent);

    /**
     * 후기 삭제
     * @param reviewId 후기 ID
     * @param userId 작성자 ID
     */
    void deleteReview(Long reviewId, Long userId);

    /**
     * 좋아요 등록
     */
    void likeReview(Long reviewId, Long userId);

    /**
     * 좋아요 취소
     */
    void unlikeReview(Long reviewId, Long userId);
}
