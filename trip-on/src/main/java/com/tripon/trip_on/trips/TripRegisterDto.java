// --- DTO 클래스 ---
package com.tripon.trip_on.trips;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
//중간 저장용(이전버튼)
public class TripRegisterDto {
    private String title; // 여행 제목 (실제로는 목적지 이름)
     @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;   // 여행 시작일

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;     // 여행 종료일
    private String accommodation;
       private String accommodationLink;
    private String departureTrip; //가는 편
    private String returnTrip; //오는 편
    private List<String> selectedTags;
    private List<String> tags;    // 예약 단계에서 사용하는 List<String>
    private String     tagsInput; // 편집 화면에서 comma-string
}