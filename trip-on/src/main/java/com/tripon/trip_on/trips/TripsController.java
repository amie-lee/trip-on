// --- Controller ---
package com.tripon.trip_on.trips;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * PlanController
 * 여행 등록 과정 (4단계 폼) 컨트롤러
 * - 목적지 → 일정 → 숙소/교통편 → 태그 순으로 입력
 * - 각 단계에서 @ModelAttribute("trip")에 저장되는 TripRegisterDto로 데이터 누적
 */
@Controller
@RequestMapping("/trips")
@SessionAttributes("tripRegisterDto")  // TripRegisterDto를 세션에 저장해 단계별 폼에서 공유
public class TripsController {

    @Autowired
    private TripsService planService;

    // 초기 trip DTO 생성 (세션에 등록됨)
    @ModelAttribute("tripRegisterDto")
    public TripRegisterDto tripRegisterDto() {
        return new TripRegisterDto();
    }

    // 1단계: 여행지 입력 폼
    @GetMapping("/register/trip-place")
    public String showPlaceForm(Model model) {
        return "trips/trip-place";
    }

    // 1단계: 여행지 입력 처리
    @PostMapping("/register/trip-place")
    public String processPlace(@ModelAttribute("tripRegisterDto") TripRegisterDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return "trips/trip-place"; // 여행지(title) 미입력 시 다시 해당 페이지
        }
        return "redirect:/trips/register/trip-schedule";
    }

    // 2단계: 일정 입력 폼
    @GetMapping("/register/trip-schedule")
    public String scheduleForm() {
        return "trips/trip-schedule";
    }

    // 2단계: 일정 입력 처리
    @PostMapping("/register/trip-schedule")
    public String processSchedule(@ModelAttribute("tripRegisterDto") TripRegisterDto dto) {
        // 시작일과 종료일이 유효할 경우 다음 단계로
        if (dto.getStartDate() != null && dto.getEndDate() != null && !dto.getEndDate().isBefore(dto.getStartDate())) {
            return "redirect:/trips/register/trip-trans";
        }
        return "trips/trip-schedule";
    }

    // 3단계: 숙소/교통 입력 폼
    @GetMapping("/register/trip-trans")
    public String accommodationForm() {
        return "trips/trip-trans";
    }

    @PostMapping("/register/trip-trans")
    public String processAccommodation(
            @ModelAttribute("tripRegisterDto") TripRegisterDto dto,
            @RequestParam(required = false) String skip) {
    
        // 건너뛰기 눌렀다면 null 처리
        if (skip != null) {
            dto.setAccommodation(null);
            dto.setTransportationDeparture(null);
            dto.setTransportationReturn(null);
        }
        // Trip 저장하지 않고 바로 태그 단계로 이동
        return "redirect:/trips/register/trip-tags";
    }

    // 4단계: 태그 선택 폼
    @GetMapping("/register/trip-tags")
    public String tagForm(Model model) {
        model.addAttribute("allTags", List.of(
            "#혼자서", "#친구랑", "#가족여행",
            "#연인과", "#휴식",   "#관광"
        ));
        return "trips/trip-tags";
    }

    @PostMapping("/register/trip-tags")
    public String submitTags(
        //체크박스에서 선택된 태그들이 배열로 넘어옴.
            @RequestParam(required = false, name = "selectedTags") String[] selectedTags,
            @ModelAttribute("tripRegisterDto") TripRegisterDto dto,
            //세션에서 로그인된 사용자 ID 꺼내기 위해 필요
            HttpSession session) {

        // 1) 로그인 사용자 ID 가져옴.
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // 2) Trip 엔티티 저장 (여기서 ID 생성)
        Trip savedTrip = planService.saveTrip(userId, dto);


        // 3) 선택된 태그가 있으면 TripTag 저장
        if (selectedTags != null) {
            planService.saveTags(savedTrip, Arrays.asList(selectedTags));
        }
        
        // 4) 세션에 남은 임시 DTO 정리 (선택)
        session.removeAttribute("tripRegisterDto");

        // 5) 저장 후 상세 페이지로 이동
        return "redirect:/trips/main-past";
    }

}

