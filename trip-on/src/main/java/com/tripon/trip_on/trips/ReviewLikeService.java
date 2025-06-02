package com.tripon.trip_on.trips;

public interface ReviewLikeService {
    void like(Long reviewId, Long userId);
    void unlike(Long reviewId, Long userId);
    long getLikeCount(Long reviewId);
}
