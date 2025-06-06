package com.tripon.trip_on.trips;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripUpdateDto {
    private String title;                     // 여행 제목
   @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private String accommodation;            // 숙소
    private String departureTrip;  // 출발 교통
    private String returnTrip;     // 귀환 교통

     /** 태그를 콤마(,)로 입력할 텍스트 박스 */
    private String tagsText;
    
    private List<String> selectedTags;       // 선택된 태그 목록
}