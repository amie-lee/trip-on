// --- DetailController.java (refactored) ---
package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.Schedule;
import com.tripon.trip_on.trips.TripTag;
import com.tripon.trip_on.trips.TripRepository;
import com.tripon.trip_on.trips.ScheduleRepository;
import com.tripon.trip_on.trips.TripTagRepository;
import com.tripon.trip_on.trips.TripsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@SessionAttributes({"tripUpdateDto","scheduleForm"})
public class DetailController {
    private static final Logger log = LoggerFactory.getLogger(DetailController.class);


    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final TripTagRepository tripTagRepository;
    private final TripsService tripsService;

    @Autowired
    public DetailController(TripRepository tripRepository,
                            ScheduleRepository scheduleRepository,
                            TripTagRepository tripTagRepository,
                            TripsService tripService) {
        this.tripRepository = tripRepository;
        this.scheduleRepository = scheduleRepository;
        this.tripTagRepository = tripTagRepository;
        this.tripsService = tripService;
    }

        @ModelAttribute("tripUpdateDto")
    public TripUpdateDto tripUpdateDto() {
        return new TripUpdateDto();
    }
    @ModelAttribute("scheduleForm")
    public ScheduleForm scheduleForm() {
        return new ScheduleForm();
    }

    @GetMapping("/trips/{tripId}/trip-plan")
public String showTripPlan(@PathVariable Long tripId, Model model) {
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
            );

    List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
    List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

    // 날짜 라벨
    List<String> dateLabels = tripsService.generateDateLabels(trip);

    // ← 여기서 dayNumber 로 묶어둡니다
    Map<Integer,List<Schedule>> scheduleMap = schedules.stream()
        .collect(Collectors.groupingBy(Schedule::getDayNumber));

    model.addAttribute("trip", trip);
    model.addAttribute("tags", tags);
    model.addAttribute("dateLabels", dateLabels);
    model.addAttribute("scheduleMap", scheduleMap);  // 추가

    return "trips/trip-plan";
}

    /** 편집 폼 조회 */
    @GetMapping("/trips/{tripId}/trip-plan-trip")
    public String showEditForm(@PathVariable Long tripId,
                               @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
                               Model model) {
        // --- 기존 상세 데이터 모델에 담기 ---
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
            );
        List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
        List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);
        List<String> dateLabels = tripsService.generateDateLabels(trip);

        model.addAttribute("trip", trip);
        model.addAttribute("schedules", schedules);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);

        // --- 편집 DTO 채우기 ---
        TripUpdateDto filled = tripsService.getTripUpdateDto(tripId);
        dto.setTitle(filled.getTitle());
        dto.setStartDate(filled.getStartDate());
        dto.setEndDate(filled.getEndDate());
        dto.setAccommodation(filled.getAccommodation());
        dto.setTransportationDeparture(filled.getTransportationDeparture());
        dto.setTransportationReturn(filled.getTransportationReturn());
        dto.setTagsText(filled.getTagsText());

        model.addAttribute("tripId", tripId);
        model.addAttribute("allTags", tripsService.getAllTagNames());

        return "trips/trip-plan-trip";
    }

    /** 편집 내용 저장 */
    @PostMapping("/trips/{tripId}/trip-plan-trip")
    public String processEdit(@PathVariable Long tripId,
                              @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
                              SessionStatus status) {
        tripsService.updateTrip(tripId, dto);
        status.setComplete();
        return "redirect:/trips/" + tripId + "/trip-plan";
    }

  @GetMapping("/trips/{tripId}/trip-plan-schedule")
public String editSchedule(
        @PathVariable Long tripId,
        @ModelAttribute("scheduleForm") ScheduleForm form,
        Model model) {

    // 1) Trip 조회
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Trip not found: " + tripId));

    // 2) 날짜 라벨, 태그 조회
    List<String> dateLabels = tripsService.generateDateLabels(trip);
    List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

    // 3) DB에서 ScheduleUpdateDto 목록 로드 후 폼에 세팅
    List<ScheduleUpdateDto> dtos = tripsService.loadSchedules(tripId);
    form.setScheduleDtos(dtos);

    // 4) 모델에 추가
    model.addAttribute("trip", trip);
    model.addAttribute("tags", tags);
    model.addAttribute("dateLabels", dateLabels);
    // scheduleForm은 @ModelAttribute로 자동 추가됨

    return "trips/trip-plan-schedule";
}

   @PostMapping("/trips/{tripId}/trip-plan-schedule")
public String saveSchedule(
        @PathVariable Long tripId,
        @ModelAttribute("scheduleForm") ScheduleForm scheduleForm,
        SessionStatus status            // ← 여기에 추가
) {
    // 1) DTO에 tripId 세팅
    List<ScheduleUpdateDto> dtos = scheduleForm.getScheduleDtos();
    dtos.forEach(dto -> dto.setTripId(tripId));

    // 2) 저장
    tripsService.saveSchedules(dtos);

    // 3) 세션에 보관된 scheduleForm 제거
    status.setComplete();

    // 4) 상세 페이지로 리다이렉트
    return "redirect:/trips/" + tripId + "/trip-plan";
}

}
