package com.tripon.trip_on.trips;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tripon.trip_on.user.User;

/**
 * 여행 후기 엔티티
 * 여행에 대한 사용자의 후기와 관련된 정보를 저장하는 엔티티 클래스
 */
@Data                   // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor      // Lombok: 파라미터가 없는 기본 생성자 자동 생성
@AllArgsConstructor     // Lombok: 모든 필드를 파라미터로 받는 생성자 자동 생성
@Builder               // Lombok: 빌더 패턴 구현을 위한 메서드 자동 생성
@Entity                // JPA: 이 클래스가 데이터베이스 테이블과 매핑되는 엔티티임을 명시
@Table(name = "review") // JPA: 실제 데이터베이스 테이블 이름을 'review'로 지정
public class Review {

    @Id                 // JPA: 이 필드가 테이블의 기본키(Primary Key)임을 명시
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA: 기본키 생성 전략을 IDENTITY(자동 증가)로 설정
    private Long id;    // 후기 고유 식별자 (PK)

    @Column(name = "trip_id", nullable = false) // JPA: trip_id 컬럼으로 매핑, null 값 허용하지 않음 (FK)
    private Long tripId; // 연관된 여행의 ID (FK)

    @Column(name = "user_id", nullable = false) // JPA: user_id 컬럼으로 매핑, null 값 허용하지 않음 (FK)
    private Long userId; // 후기 작성자(유저) ID (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false) // JPA: TEXT 타입으로 매핑, null 값 허용하지 않음 (글)
    private String content; // 후기 내용 (글)

    @ElementCollection(fetch = FetchType.LAZY) // JPA: 컬렉션 타입 매핑, 지연 로딩 설정 (후기 첨부파일 목록)
    @CollectionTable(name = "review_files", joinColumns = @JoinColumn(name = "review_id")) // JPA: 파일 경로를 저장할 별도 테이블 설정, review_id로 조인
    @Column(name = "file_path") // JPA: 파일 경로를 저장할 컬럼명 지정
    @Builder.Default    // Lombok: 빌더 패턴 사용 시 기본값 설정
    private List<String> filePaths = new ArrayList<>(); // 후기에 첨부된 파일들의 경로 목록

    @Column(name = "created_at", nullable = false, updatable = false) // JPA: 생성 시간 컬럼, null 불가, 수정 불가
    private LocalDateTime createdAt; // 후기 작성 시간

    @Column(name = "update_at")
    private LocalDateTime updateAt; // 후기 수정 시간

    @Transient // DB에 저장하지 않는 임시 필드
    private boolean liked; // 현재 사용자가 좋아요를 눌렀는지 여부

    @PrePersist         // JPA: 엔티티가 저장되기 전에 실행될 메서드 지정
    protected void onPrePersist() {
        this.createdAt = LocalDateTime.now(); // 엔티티가 저장될 때 현재 시간을 생성 시간으로 자동 설정
    }

    @PreUpdate // JPA: 엔티티가 수정되기 전에 실행될 메서드 지정
    protected void onPreUpdate() {
        this.updateAt = LocalDateTime.now(); // 엔티티가 수정될 때 현재 시간을 수정 시간으로 자동 설정
    }

    @Transient
    private long likeCount;

    // getter / setter
    public long getLikeCount() {
        return likeCount;
    }
    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }
}
