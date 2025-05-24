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

import java.util.List;

@Controller
@SessionAttributes("tripUpdateDto")
public class DetailController {


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


    /**
     * 여행 상세 페이지 조회
     */
    @GetMapping("/trips/{tripId}/trip-plan")
    public String showTripPlan(@PathVariable Long tripId, Model model) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );

        List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
        List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

        // 서비스로 날짜 라벨 생성
        List<String> dateLabels = tripsService.generateDateLabels(trip);

        model.addAttribute("trip", trip);
        model.addAttribute("schedules", schedules);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);

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
    public String editSchedule(@PathVariable Long tripId, Model model) {
        // 공통: trip, dateLabels
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<String> dateLabels = tripsService.generateDateLabels(trip);
        List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

        // 기존 및 바인딩용 DTO
        List<ScheduleUpdateDto> scheduleDtos = tripsService.loadSchedules(tripId);

        model.addAttribute("trip", trip);
         model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("scheduleDtos", scheduleDtos);
        return "trips/trip-plan-schedule";
    }

    @PostMapping("/trips/{tripId}/trip-plan-schedule")
public String saveSchedule(
        @PathVariable Long tripId,
        @ModelAttribute("scheduleDtos") List<ScheduleUpdateDto> scheduleDtos
) {
    // tripId 세팅
    scheduleDtos.forEach(dto -> dto.setTripId(tripId));
    tripsService.saveSchedules(scheduleDtos);
    // 수정: 반드시 redirect:/ 로 시작
    return "redirect:/trips/" + tripId + "/trip-plan";
}
}
