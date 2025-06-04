package com.tripon.trip_on.trips;

import org.springframework.data.jpa.repository.JpaRepository; // JPA 레포지토리
import java.util.List;

// 여행 후기 관련 DB 접근 레포지토리 (JPA)
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 여행(tripId)에 대한 후기 목록 조회
    List<Review> findByTripId(Long tripId);
} 