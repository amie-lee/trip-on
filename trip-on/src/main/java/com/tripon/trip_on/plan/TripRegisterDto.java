// --- DTO 클래스 ---
package com.tripon.trip_on.plan;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
//중간 저장용(이전버튼)
public class TripRegisterDto {
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String accommodation;
    private String transportationDeparture;
    private String transportationReturn;
    private List<String> selectedTags;
}