package com.tripon.trip_on.trips;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    /**
     * 로그인한 사용자가 생성한 모든 Trip을 조회합니다.
     */
    List<Trip> findByCreatorId(Long creatorId);
    
    boolean existsByCreatorId(Long creatorId);
}
