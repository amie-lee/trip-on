// // --- DetailController.java (refactored) ---
// package com.tripon.trip_on.trips;

// import com.tripon.trip_on.trips.Trip;
// import com.tripon.trip_on.trips.Schedule;
// import com.tripon.trip_on.trips.TripTag;
// import com.tripon.trip_on.trips.TripRepository;
// import com.tripon.trip_on.trips.ScheduleRepository;
// import com.tripon.trip_on.trips.TripTagRepository;
// import com.tripon.trip_on.trips.TripsService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.ModelAttribute;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.SessionAttributes;
// import org.springframework.web.bind.support.SessionStatus;
// import org.springframework.web.server.ResponseStatusException;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Controller
// @SessionAttributes({"tripUpdateDto","scheduleForm"})
// public class DetailController {
//     private static final Logger log = LoggerFactory.getLogger(DetailController.class);


//     private final TripRepository tripRepository;
//     private final ScheduleRepository scheduleRepository;
//     private final TripTagRepository tripTagRepository;
//     private final TripsService tripsService;

//     @Autowired
//     public DetailController(TripRepository tripRepository,
//                             ScheduleRepository scheduleRepository,
//                             TripTagRepository tripTagRepository,
//                             TripsService tripService) {
//         this.tripRepository = tripRepository;
//         this.scheduleRepository = scheduleRepository;
//         this.tripTagRepository = tripTagRepository;
//         this.tripsService = tripService;
//     }

//         @ModelAttribute("tripUpdateDto")
//     public TripUpdateDto tripUpdateDto() {
//         return new TripUpdateDto();
//     }
//     @ModelAttribute("scheduleForm")
//     public ScheduleForm scheduleForm() {
//         return new ScheduleForm();
//     }

//     @GetMapping("/trips/{tripId}/trip-plan")
// public String showTripPlan(@PathVariable Long tripId, Model model) {
//     Trip trip = tripRepository.findById(tripId)
//             .orElseThrow(() ->
//                 new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
//             );

//     List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
//     List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

//     // 날짜 라벨
//     List<String> dateLabels = tripsService.generateDateLabels(trip);

//     // ← 여기서 dayNumber 로 묶어둡니다
//     Map<Integer,List<Schedule>> scheduleMap = schedules.stream()
//         .collect(Collectors.groupingBy(Schedule::getDayNumber));

//     model.addAttribute("trip", trip);
//     model.addAttribute("tags", tags);
//     model.addAttribute("dateLabels", dateLabels);
//     model.addAttribute("scheduleMap", scheduleMap);  // 추가

//     return "trips/trip-plan";
// }

//     @GetMapping("/trips/{tripId}/trip-plan-trip")
// public String showEditForm(@PathVariable Long tripId,
//                            @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
//                            Model model) {
//     Trip trip = tripRepository.findById(tripId)
//         .orElseThrow(() ->
//             new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
//         );
//     List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
//     List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);
//     List<String> dateLabels = tripsService.generateDateLabels(trip);

//     // --- 모델에 기본 데이터 ---
//     model.addAttribute("trip", trip);
//     model.addAttribute("schedules", schedules);
//     model.addAttribute("tags", tags);
//     model.addAttribute("dateLabels", dateLabels);
//     model.addAttribute("tripId", tripId);
//     model.addAttribute("allTags", tripsService.getAllTagNames());

//     // 🔥 여기서 scheduleMap 추가
//     Map<Integer, List<Schedule>> scheduleMap = schedules.stream()
//         .collect(Collectors.groupingBy(Schedule::getDayNumber));
//     model.addAttribute("scheduleMap", scheduleMap);

//     // --- DTO 채우기 ---
//     TripUpdateDto filled = tripsService.getTripUpdateDto(tripId);
//     dto.setTitle(filled.getTitle());
//     dto.setStartDate(filled.getStartDate());
//     dto.setEndDate(filled.getEndDate());
//     dto.setAccommodation(filled.getAccommodation());
//     dto.setTransportationDeparture(filled.getTransportationDeparture());
//     dto.setTransportationReturn(filled.getTransportationReturn());
//     dto.setTagsText(filled.getTagsText());

//     return "trips/trip-plan-trip";
// }

//     /** 편집 내용 저장 */
//     @PostMapping("/trips/{tripId}/trip-plan-trip")
//     public String processEdit(@PathVariable Long tripId,
//                               @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
//                               SessionStatus status) {
//         tripsService.updateTrip(tripId, dto);
//         status.setComplete();
//         return "redirect:/trips/" + tripId + "/trip-plan";
//     }

//   @GetMapping("/trips/{tripId}/trip-plan-schedule")
// public String editSchedule(
//         @PathVariable Long tripId,
//         @ModelAttribute("scheduleForm") ScheduleForm form,
//         Model model) {

//     // 1) Trip 조회
//     Trip trip = tripRepository.findById(tripId)
//         .orElseThrow(() -> new ResponseStatusException(
//                 HttpStatus.NOT_FOUND, "Trip not found: " + tripId));

//     // 2) 날짜 라벨, 태그 조회
//     List<String> dateLabels = tripsService.generateDateLabels(trip);
//     List<TripTag> tags      = tripTagRepository.findAllByTripId(tripId);

//     // 3) DB에서 ScheduleUpdateDto 목록 로드 후 폼에 세팅
//     List<ScheduleUpdateDto> dtos = tripsService.loadSchedules(tripId);
//     form.setScheduleDtos(dtos);

//     // 4) 모델에 추가
//     model.addAttribute("trip", trip);
//     model.addAttribute("tags", tags);
//     model.addAttribute("dateLabels", dateLabels);
//     // scheduleForm은 @ModelAttribute로 자동 추가됨

//     return "trips/trip-plan-schedule";
// }

//    @PostMapping("/trips/{tripId}/trip-plan-schedule")
// public String saveSchedule(
//         @PathVariable Long tripId,
//         @ModelAttribute("scheduleForm") ScheduleForm scheduleForm,
//         SessionStatus status            // ← 여기에 추가
// ) {
//     // 1) DTO에 tripId 세팅
//     List<ScheduleUpdateDto> dtos = scheduleForm.getScheduleDtos();
//     dtos.forEach(dto -> dto.setTripId(tripId));

//     // 2) 저장
//     tripsService.saveSchedules(dtos);

//     // 3) 세션에 보관된 scheduleForm 제거
//     status.setComplete();

//     // 4) 상세 페이지로 리다이렉트
//     return "redirect:/trips/" + tripId + "/trip-plan";
// }

// }

// --- DetailController.java (헤더에 user 추가된 버전) ---
package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.Trip;
import com.tripon.trip_on.trips.Schedule;
import com.tripon.trip_on.trips.TripTag;
import com.tripon.trip_on.trips.TripRepository;
import com.tripon.trip_on.trips.ScheduleRepository;
import com.tripon.trip_on.trips.TripTagRepository;
import com.tripon.trip_on.trips.TripsService;
import com.tripon.trip_on.user.User;         // ← 추가
import com.tripon.trip_on.user.UserService;  // ← 추가

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

import jakarta.servlet.http.HttpSession;      // ← 추가
import java.util.List;
import java.util.Map;
import java.util.Optional;                   // ← 추가
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
    private final UserService userService;  // ← UserService 주입

    @Autowired
    public DetailController(TripRepository tripRepository,
                            ScheduleRepository scheduleRepository,
                            TripTagRepository tripTagRepository,
                            TripsService tripService,
                            UserService userService) {   // ← 생성자에 UserService 추가
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
    public String showTripPlan(@PathVariable Long tripId,
                               HttpSession session,    // ← 세션을 받아야 userId 체크 가능
                               Model model) {
        // ① 반드시 세션에서 로그인된 userId 확인
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ② userId → User 조회 후 모델에 추가(헤더에 사용)
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", optUser.get());

        // ③ Trip, Schedule, Tag, 날짜 라벨 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );

        List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
        List<TripTag> tags = tripTagRepository.findAllByTripId(tripId);
        List<String> dateLabels = tripsService.generateDateLabels(trip);
        Map<Integer, List<Schedule>> scheduleMap = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDayNumber));

        // ④ 모델에 추가
        model.addAttribute("trip", trip);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("scheduleMap", scheduleMap);

        return "trips/trip-plan";
    }

    // ── (2) 여행 정보 편집 폼 보기 ──
    @GetMapping("/trips/{tripId}/trip-plan-trip")
    public String showEditForm(@PathVariable Long tripId,
                               @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
                               HttpSession session,   // ← 세션을 받아야 userId 체크 가능
                               Model model) {

        // ① 세션에서 userId 체크
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ② User 조회 후 모델에 추가(헤더에 사용)
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", optUser.get());

        // ③ Trip, Schedule, Tag, 날짜 라벨 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );
        List<Schedule> schedules = scheduleRepository.findAllByTripId(tripId);
        List<TripTag> tags = tripTagRepository.findAllByTripId(tripId);
        List<String> dateLabels = tripsService.generateDateLabels(trip);

        // ④ 모델에 기본 데이터 추가
        model.addAttribute("trip", trip);
        model.addAttribute("schedules", schedules);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("tripId", tripId);
        model.addAttribute("allTags", tripsService.getAllTagNames());

        // ⑤ scheduleMap 추가
        Map<Integer, List<Schedule>> scheduleMap = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDayNumber));
        model.addAttribute("scheduleMap", scheduleMap);

        // ⑥ DTO 채우기
        TripUpdateDto filled = tripsService.getTripUpdateDto(tripId);
        dto.setTitle(filled.getTitle());
        dto.setStartDate(filled.getStartDate());
        dto.setEndDate(filled.getEndDate());
        dto.setAccommodation(filled.getAccommodation());
        dto.setTransportationDeparture(filled.getTransportationDeparture());
        dto.setTransportationReturn(filled.getTransportationReturn());
        dto.setTagsText(filled.getTagsText());

        return "trips/trip-plan-trip";
    }

    /** ── (3) 편집 내용 저장 ── */
    @PostMapping("/trips/{tripId}/trip-plan-trip")
    public String processEdit(@PathVariable Long tripId,
                              @ModelAttribute("tripUpdateDto") TripUpdateDto dto,
                              SessionStatus status) {
        tripsService.updateTrip(tripId, dto);
        status.setComplete();
        return "redirect:/trips/" + tripId + "/trip-plan";
    }

    // ── (4) 일정 편집 폼 보기 ──
    @GetMapping("/trips/{tripId}/trip-plan-schedule")
    public String editSchedule(@PathVariable Long tripId,
                               @ModelAttribute("scheduleForm") ScheduleForm form,
                               HttpSession session,  // ← 세션을 받아야 userId 체크 가능
                               Model model) {

        // ① 세션에서 userId 체크
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ② User 조회 후 모델에 추가(헤더에 사용)
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", optUser.get());

        // ③ Trip, 날짜 라벨, 태그, DTO 목록 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId)
                );
        List<String> dateLabels = tripsService.generateDateLabels(trip);
        List<TripTag> tags = tripTagRepository.findAllByTripId(tripId);
        List<ScheduleUpdateDto> dtos = tripsService.loadSchedules(tripId);
        form.setScheduleDtos(dtos);

        // ④ 모델에 추가
        model.addAttribute("trip", trip);
        model.addAttribute("tags", tags);
        model.addAttribute("dateLabels", dateLabels);

        return "trips/trip-plan-schedule";
    }

    // ── (5) 일정 저장 처리 ──
    @PostMapping("/trips/{tripId}/trip-plan-schedule")
    public String saveSchedule(@PathVariable Long tripId,
                               @ModelAttribute("scheduleForm") ScheduleForm scheduleForm,
                               SessionStatus status) {
        // ① DTO 에 tripId 세팅
        List<ScheduleUpdateDto> dtos = scheduleForm.getScheduleDtos();
        dtos.forEach(dto -> dto.setTripId(tripId));

        // ② 저장
        tripsService.saveSchedules(dtos);

        // ③ 세션에 보관된 scheduleForm 제거
        status.setComplete();

        // ④ 상세 페이지로 리다이렉트
        return "redirect:/trips/" + tripId + "/trip-plan";
    }
}
