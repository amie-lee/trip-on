package com.tripon.trip_on.trips;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    // 특정 Trip의 모든 참여자 조회
    List<TripMember> findByTripId(Long tripId);

    // 특정 사용자와 Trip의 관계 존재 여부
    boolean existsByTripIdAndUserId(Long tripId, Long userId);
}
