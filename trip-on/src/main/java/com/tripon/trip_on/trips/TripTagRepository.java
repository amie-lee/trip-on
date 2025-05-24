package com.tripon.trip_on.trips;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TripTagRepository extends JpaRepository<TripTag, Long> {
    // 기존 메서드
    List<TripTag> findByTripId(Long tripId);
    List<TripTag> findByTripIdOrderById(Long tripId);
    void deleteAllByTrip(Trip trip);

    // 추가: findAllByTripId (findByTripId와 동일 동작)
    List<TripTag> findAllByTripId(Long tripId);

    // 중복 없는 태그명 조회
    @Query("SELECT DISTINCT t.tagName FROM TripTag t")
    List<String> findDistinctTagNames();
}