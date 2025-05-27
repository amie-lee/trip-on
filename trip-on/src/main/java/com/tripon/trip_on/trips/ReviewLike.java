package com.tripon.trip_on.trips;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_like")
@IdClass(ReviewLikeId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLike {
    @Id
    @Column(name = "review_id")
    private Long reviewId;

    @Id
    @Column(name = "user_id")
    private Long userId;
} 