package com.tripon.trip_on.trips;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {
    List<ReviewPhoto> findByReviewId(Long reviewId);
} 