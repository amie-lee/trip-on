package com.tripon.trip_on.trips;

import jakarta.persistence.*; // JPA 어노테이션
import lombok.*; // Lombok 어노테이션

@Entity // JPA: 이 클래스가 테이블과 매핑됨을 명시
@Table(name = "review_like") // 실제 테이블명 지정
@IdClass(ReviewLikeId.class) // 복합키 매핑용 ID 클래스 지정
@Data // Lombok: getter/setter 등 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드 생성자 자동 생성
public class ReviewLike {
    @Id // JPA: 복합키(리뷰ID)
    @Column(name = "review_id") // 컬럼명 지정
    private Long reviewId; // 좋아요가 눌린 리뷰의 ID

    @Id // JPA: 복합키(유저ID)
    @Column(name = "user_id") // 컬럼명 지정
    private Long userId; // 좋아요를 누른 유저의 ID
} 