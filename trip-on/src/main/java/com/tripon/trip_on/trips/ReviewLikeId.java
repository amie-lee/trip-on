package com.tripon.trip_on.trips;

import java.io.Serializable; // 직렬화 인터페이스
import lombok.*; // Lombok 어노테이션

@Data // Lombok: getter/setter 등 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드 생성자 자동 생성
public class ReviewLikeId implements Serializable {
    private Long reviewId; // 리뷰 ID (복합키)
    private Long userId;   // 유저 ID (복합키)
} 