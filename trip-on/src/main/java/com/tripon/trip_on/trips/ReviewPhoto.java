package com.tripon.trip_on.trips;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_photo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl; // 실제 파일 경로

    @Column(name = "file_path", nullable = false)
    private String filePath; // 파일 시스템 경로

    @Column(name = "file_type", nullable = false)
    private String fileType; // "image" 또는 "video"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }
} 