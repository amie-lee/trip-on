// --- DetailController.java (login 체크 추가 버전) ---
package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.Schedule;
import com.tripon.trip_on.trips.TripTag;
import com.tripon.trip_on.trips.TripRepository;
import com.tripon.trip_on.trips.ScheduleRepository;
import com.tripon.trip_on.trips.TripTagRepository;
import com.tripon.trip_on.trips.TripsService;
import com.tripon.trip_on.user.User;         // ← User 엔티티
import com.tripon.trip_on.user.UserService;  // ← UserService

import jakarta.servlet.http.HttpSession;      // ← HttpSession
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
import java.util.Map;
import java.util.Optional;
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
    private final UserService userService;  // ← UserService 추가

    @Autowired
    public DetailController(TripRepository tripRepository,
                            ScheduleRepository scheduleRepository,
                            TripTagRepository tripTagRepository,
                            TripsService tripService,
                            UserService userService) { // ← 생성자에 UserService 추가
        this.tripRepository = tripRepository;
        this.scheduleRepository = scheduleRepository;
        this.tripTagRepository = tripTagRepository;
        this.tripsService = tripService;
        this.userService = userService;
    }

    @ModelAttribute("tripUpdateDto")
    public TripUpdateDto tripUpdateDto() {
        return new TripUpdateDto();
    }

    @ModelAttribute("scheduleForm")
    public ScheduleForm scheduleForm() {
        return new ScheduleForm();
    }

    // ── (1) 여행 계획 상세 보기 ──
    @GetMapping("/trips/{tripId}/trip-plan")
    public String showTripPlan(
            @PathVariable Long tripId,
            HttpSession session,    // ← 세션 파라미터 추가
            Model model) {

        // ① 세션에서 로그인 사용자 ID 확인
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ② UserService로 User 조회. 없으면 세션 무효화 후 로그인 페이지로
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        // ③ 헤더에 표시할 User 객체를 모델에 추가
        model.addAttribute("user", optUser.get());

        // ── 이하 기존 로직 ──
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );

        List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
        List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

        // 날짜 라벨 생성
        List<String> dateLabels = tripsService.generateDateLabels(trip);

        // dayNumber로 그룹화
        Map<Integer, List<Schedule>> scheduleMap = schedules.stream()
            .collect(Collectors.groupingBy(Schedule::getDayNumber));

        model.addAttribute("trip", trip);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("scheduleMap", scheduleMap);

        return "trips/trip-plan";
    }

    // ── (2) 여행 정보 편집 폼 보기 ──
    @GetMapping("/trips/{tripId}/trip-plan-trip")
    public String showEditForm(
            @PathVariable Long tripId,
            @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
            HttpSession session,   // ← 세션 파라미터 추가
            Model model) {

        // ① 세션에서 로그인 사용자 ID 확인
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ② UserService로 User 조회. 없으면 세션 무효화 후 로그인 페이지로
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        // ③ 헤더 표시용 User 객체 모델에 추가
        model.addAttribute("user", optUser.get());

        // ── 이하 기존 로직 ──
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );
        List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
        List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);
        List<String> dateLabels = tripsService.generateDateLabels(trip);

        // 기본 모델 데이터
        model.addAttribute("trip", trip);
        model.addAttribute("schedules", schedules);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("tripId", tripId);
        model.addAttribute("allTags", tripsService.getAllTagNames());

        // scheduleMap 추가
        Map<Integer, List<Schedule>> scheduleMap = schedules.stream()
            .collect(Collectors.groupingBy(Schedule::getDayNumber));
        model.addAttribute("scheduleMap", scheduleMap);

        // DTO 채우기
        TripUpdateDto filled = tripsService.getTripUpdateDto(tripId);
        dto.setTitle(filled.getTitle());
        dto.setStartDate(filled.getStartDate());
        dto.setEndDate(filled.getEndDate());
        dto.setAccommodation(filled.getAccommodation());
        dto.setDepartureTrip(filled.getDepartureTrip());
        dto.setReturnTrip(filled.getReturnTrip());
        dto.setTagsText(filled.getTagsText());

        return "trips/trip-plan-trip";
    }

    /** ── (3) 편집 내용 저장 ── */
    @PostMapping("/trips/{tripId}/trip-plan-trip")
    public String processEdit(
            @PathVariable Long tripId,
            @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
            SessionStatus status) {

        tripsService.updateTrip(tripId, dto);
        status.setComplete();
        return "redirect:/trips/" + tripId + "/trip-plan";
    }

    // ── (4) 일정 편집 폼 보기 ──
    @GetMapping("/trips/{tripId}/trip-plan-schedule")
    public String editSchedule(
            @PathVariable Long tripId,
            @ModelAttribute("scheduleForm") ScheduleForm form,
            HttpSession session,  // ← 세션 파라미터 추가
            Model model) {

        // ① 세션에서 로그인 사용자 ID 확인
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ② UserService로 User 조회. 없으면 세션 무효화 후 로그인 페이지로
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        // ③ 헤더 표시용 User 객체 모델에 추가
        model.addAttribute("user", optUser.get());

        // ── 이하 기존 로직 ──
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );
        List<String> dateLabels = tripsService.generateDateLabels(trip);
        List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

        // DTO 목록 로드
        List<ScheduleUpdateDto> dtos = tripsService.loadSchedules(tripId);
        form.setScheduleDtos(dtos);

        model.addAttribute("trip", trip);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);

        return "trips/trip-plan-schedule";
    }

    // ── (5) 일정 저장 처리 ──
    @PostMapping("/trips/{tripId}/trip-plan-schedule")
    public String saveSchedule(
            @PathVariable Long tripId,
            @ModelAttribute("scheduleForm") ScheduleForm scheduleForm,
            SessionStatus status) {

        // DTO에 tripId 세팅
        List<ScheduleUpdateDto> dtos = scheduleForm.getScheduleDtos();
        dtos.forEach(dto -> dto.setTripId(tripId));

        // 저장
        tripsService.saveSchedules(dtos);

        // 세션에 보관된 scheduleForm 제거
        status.setComplete();

        return "redirect:/trips/" + tripId + "/trip-plan";
    }
}
