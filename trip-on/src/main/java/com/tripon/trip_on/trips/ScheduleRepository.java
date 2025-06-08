package com.tripon.trip_on.trips;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByTripId(Long tripId);
    List<Schedule> findAllByTripIdOrderByDayNumberAscTimeAsc(Long tripId);
    void deleteByTripId(Long tripId);
}