// --- Service ---
package com.tripon.trip_on.plan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * PlanService - 여행 등록 및 태그 저장 로직 처리
 */
@Service
public class PlanService {

    @Autowired
    private TripRepository TripRepository;

    @Autowired
    private TripTagRepository tripTagRepository;

    /**
     * 여행 정보 저장
     * @param userId 로그인한 유저의 ID
     * @param dto 여행 등록 화면에서 받은 데이터
     * @return 저장된 Trip 객체
     */
    public Trip saveTrip(Long userId, TripRegisterDto dto) {
        Trip trip = new Trip();
        trip.setTitle(dto.getDestination());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setAccommodation(dto.getAccommodation());

        // 교통편 정보는 '출발 ~ 도착' 형식으로 문자열로 저장
        trip.setTransportaion(dto.getTransportationDeparture() + " ~ " + dto.getTransportationReturn());

        trip.setStatus("예정"); // 기본 상태는 '예정'
        trip.setCreatorId(userId); // 로그인한 사용자가 생성자

        return TripRepository.save(trip); // DB에 저장
    }

    /**
     * 태그 목록 저장 (Trip과 1:N 관계)
     * @param trip 저장된 Trip 객체
     * @param tags 태그 문자열 리스트
     */
    public void saveTags(Trip trip, List<String> tags) {
        for (String tagName : tags) {
            TripTag tag = new TripTag();
            tag.setTrip(trip); // 외래키 설정
            tag.setTagName(tagName);
            tripTagRepository.save(tag); // DB에 저장
        }
    }
}
