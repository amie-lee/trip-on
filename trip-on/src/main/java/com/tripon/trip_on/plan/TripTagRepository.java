// --- Repository ---
package com.tripon.trip_on.plan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripTagRepository extends JpaRepository<TripTag, Long> {
    List<TripTag> findByTripId(Long tripId);
}