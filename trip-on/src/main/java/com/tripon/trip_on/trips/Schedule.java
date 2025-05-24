package com.tripon.trip_on.trips;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 Schedule이 하나의 Trip에 속함
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private int dayNumber; // 1일차, 2일차 등

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
}
