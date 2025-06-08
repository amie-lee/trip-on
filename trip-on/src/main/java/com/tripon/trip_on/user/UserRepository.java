package com.tripon.trip_on.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// User 테이블에 접근하는 인터페이스
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT m.user FROM TripMember m WHERE m.trip.id = :tripId")
    List<User> findUsersByTripId(@Param("tripId") Long tripId);
}
