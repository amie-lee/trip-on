package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.TripTag;

import jakarta.persistence.EntityNotFoundException;

import com.tripon.trip_on.trips.TripRegisterDto;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripsService {

    @Autowired private TripRepository tripRepository;
    @Autowired private TripTagRepository tripTagRepository;
    @Autowired private ScheduleRepository scheduleRepository;

    /** 로그인 유저의 Trip 목록 */
    public List<Trip> findByCreatorId(Long userId) {
        return tripRepository.findByCreatorId(userId);
    }

    /** 특정 Trip의 태그명 리스트 */
    public List<String> getTags(Long tripId) {
        return tripTagRepository.findByTripIdOrderById(tripId).stream()
                .map(TripTag::getTagName)
                .collect(Collectors.toList());
    }

    /** 전체 사용 가능한 태그명 */
    public List<String> getAllTagNames() {
        return tripTagRepository.findDistinctTagNames();
    }

    /** 새로운 Trip 저장 */
    public Trip saveTrip(Long userId, TripRegisterDto dto) {
        Trip trip = new Trip();
        trip.setTitle(dto.getTitle());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setAccommodation(dto.getAccommodation());
        // 교통편이 빈 문자열일 경우 null로 처리
        if (dto.getTransportationDeparture() != null && dto.getTransportationDeparture().isBlank()) {
            dto.setTransportationDeparture(null);
        }
        if (dto.getTransportationReturn() != null && dto.getTransportationReturn().isBlank()) {
            dto.setTransportationReturn(null);
        }
        trip.setTransportation(dto.getTransportationDeparture() + " ~ " + dto.getTransportationReturn());
        trip.setStatus("예정");
        trip.setCreatorId(userId);
        return tripRepository.save(trip);
    }

    /** TripTag 저장 */
    public void saveTags(Trip trip, List<String> tags) {
        tags.forEach(tagName -> {
            TripTag tag = new TripTag();
            tag.setTrip(trip);
            tag.setTagName(tagName);
            tripTagRepository.save(tag);
        });
    }

    /**
     * Trip의 시작일과 종료일을 이용해 날짜 라벨 리스트 생성
     * 예: "4/1 (1일차)", "4/2 (2일차)", ...
     * @param trip Trip 엔티티
     * @return 날짜 라벨 리스트
     */
    public List<String> generateDateLabels(Trip trip) {
        LocalDate start = trip.getStartDate();
        LocalDate end   = trip.getEndDate();

        List<String> dateLabels = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            int dayNumber = (int) ChronoUnit.DAYS.between(start, date) + 1;
            String label = date.getMonthValue() + "/" + date.getDayOfMonth()
                    + " (" + dayNumber + "일차)";
            dateLabels.add(label);
        }
        return dateLabels;
    }

    /** 편집 폼용 DTO 생성 */
    @Transactional(readOnly = true)
    public TripUpdateDto getTripUpdateDto(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + tripId));

        // 기존 태그들에서 '#' 빼고 콤마로 합침
        String tagsText = tripTagRepository.findAllByTripId(tripId).stream()
            .map(TripTag::getTagName)
            .map(name -> name.startsWith("#") ? name.substring(1) : name)
            .collect(Collectors.joining(","));

        String dep = "", ret = "";
        if (trip.getTransportation() != null) {
            String[] parts = trip.getTransportation().split(" ~ ");
            dep = parts.length>0?parts[0]:"";
            ret = parts.length>1?parts[1]:"";
        }

        return TripUpdateDto.builder()
            .title(trip.getTitle())
            .startDate(trip.getStartDate())
            .endDate(trip.getEndDate())
            .accommodation(trip.getAccommodation())
            .transportationDeparture(dep)
            .transportationReturn(ret)
            .tagsText(tagsText)
            .build();
    }

    /** 수정 처리: 엔티티와 태그 업데이트 */
    @Transactional
    public void updateTrip(Long tripId, TripUpdateDto dto) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + tripId));

        trip.setTitle(dto.getTitle());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setAccommodation(dto.getAccommodation());
        trip.setTransportation(dto.getTransportationDeparture()
                               + " ~ " + dto.getTransportationReturn());

        // 기존 태그 삭제
        tripTagRepository.deleteAllByTrip(trip);
        // 새 태그 저장
        if (dto.getTagsText() != null && !dto.getTagsText().isBlank()) {
            Arrays.stream(dto.getTagsText().split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .map(s -> s.startsWith("#") ? s : "#" + s)
                  .forEach(tagName -> {
                      TripTag tag = new TripTag();
                      tag.setTrip(trip);
                      tag.setTagName(tagName);
                      tripTagRepository.save(tag);
                  });
        }
    }

   /** 특정 여행의 기존 일정 DTO 리스트 로드 (day/time으로 정렬) */
    public List<ScheduleUpdateDto> loadSchedules(Long tripId) {
        return scheduleRepository.findAllByTripIdOrderByDayNumberAscTimeAsc(tripId).stream()
            .map(s -> ScheduleUpdateDto.builder()
                .id(s.getId())
                .tripId(tripId)
                .dayNumber(s.getDayNumber())
                .time(s.getTime())
                .content(s.getContent())
                .toDelete(false)
                .build())
            .collect(Collectors.toList());
    }

    /** ScheduleUpdateDto 리스트를 DB에 반영 (생성·삭제·수정) */
    @Transactional
    public void saveSchedules(List<ScheduleUpdateDto> dtos) {
        for (ScheduleUpdateDto dto : dtos) {
            if (dto.getId() != null) {
                // ── 기존 일정 ──
                if (dto.isToDelete()) {
                    // 삭제 플래그가 세팅된 항목 삭제
                    scheduleRepository.deleteById(dto.getId());
                } else {
                    // 수정: 엔티티 로드 후 필드 업데이트
                    Schedule existing = scheduleRepository.findById(dto.getId())
                        .orElseThrow(() -> new EntityNotFoundException(
                             "Schedule not found: " + dto.getId()));
                    existing.setDayNumber(dto.getDayNumber());
                    existing.setTime(dto.getTime());
                    existing.setContent(dto.getContent());
                    // 별도 save() 호출 없이, 트랜잭션 커밋 시 변경 반영됨
                }
            } else {
                // ── 신규 일정 ──
                if (!dto.isToDelete()) {
                    Schedule s = new Schedule();
                    // 프록시 참조로 Trip 세팅
                    Trip t = tripRepository.getReferenceById(dto.getTripId());
                    s.setTrip(t);
                    s.setDayNumber(dto.getDayNumber());
                    s.setTime(dto.getTime());
                    s.setContent(dto.getContent());
                    scheduleRepository.save(s);
                }
            }
        }
    }

}