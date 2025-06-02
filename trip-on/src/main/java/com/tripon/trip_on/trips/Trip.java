// --- Entity (Trip) ---
package com.tripon.trip_on.trips;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.tripon.trip_on.user.User;

/**
 * Trip 엔티티 - 여행 정보를 담는 클래스
 */
@Entity
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 여행 고유 ID

    private String title; // 여행 제목 (실제로는 목적지 이름)

    private LocalDate startDate; // 여행 시작일
    private LocalDate endDate;   // 여행 종료일

    private String accommodation;  // 숙소 정보

    private String departureTrip; // 가는 편
    private String returnTrip; // 오는 편

    private String status; // 여행 상태 (예정/완료 등)

    private Long creatorId; // 여행 생성한 유저의 ID

    @Transient
    private List<String> tags = new ArrayList<>(); // DB에 저장하지 않고 일시적으로 사용하는 태그 목록

     @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripMember> tripMembers = new ArrayList<>();

    public List<User> getTripMembers() {
        return tripMembers.stream()
                .map(TripMember::getUser)
                .toList();
    }
}
