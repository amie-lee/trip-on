package com.tripon.trip_on.trips;

import lombok.*;
import java.time.LocalTime;

/**
 * 데이터 바인딩용 DTO: 일차, 시간, 내용, 기존 데이터 업데이트 또는 삭제 표시용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ScheduleUpdateDto {
    private Long id;           // null이면 신규
    private Long tripId;       // 외래키
    private Integer dayNumber; // 1,2,...
    private LocalTime time;    // null 허용
    private String content;    // 필수
    private boolean toDelete;  // 삭제 표시
}