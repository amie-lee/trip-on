// --- DTO 클래스 ---
package com.tripon.trip_on.trips;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
//중간 저장용(이전버튼)
public class TripRegisterDto {
    private String title; // 여행 제목 (실제로는 목적지 이름)
    private LocalDate startDate; // 여행 시작일
    private LocalDate endDate;   // 여행 종료일
    private String accommodation;
    //출발 편과 도착 편을 planservice.java에서 메서드로 savetrip할 때, ~로 하나의 transportation 필드 값으로 만들어주기에
    //DTO에서는 2개의 필드가 필요함.
    private String transportationDeparture;
    private String transportationReturn;
    private List<String> selectedTags;
    private List<String> tags;    // 예약 단계에서 사용하는 List<String>
    private String     tagsInput; // 편집 화면에서 comma-string
}