// --- Repository ---
package com.tripon.trip_on.plan;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
    boolean existsByCreatorId(Long creatorId);
    List<Trip> findByCreatorId(Long creatorId);
}
