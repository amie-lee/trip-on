package com.tripon.trip_on.trips;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLikeId implements Serializable {
    private Long reviewId;
    private Long userId;
} 