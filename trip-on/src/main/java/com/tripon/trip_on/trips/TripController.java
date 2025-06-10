// --- Controller ---
package com.tripon.trip_on.trips;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import com.tripon.trip_on.user.UserRepository;


import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/trips")
@SessionAttributes("tripRegisterDto")  // TripRegisterDto를 세션에 저장해 단계별 폼에서 공유
public class TripController {

    @Autowired
    private TripService planService;

    @Autowired
    private UserRepository userRepository;  // ← UserRepository 주입

    // 초기 trip DTO 생성 (세션에 등록됨)
    @ModelAttribute("tripRegisterDto")
    public TripRegisterDto tripRegisterDto() {
        return new TripRegisterDto();
    }

    @GetMapping("/register/trip-register")
    public String showTripRegisterPage(Model model, HttpSession session) {
        // 1) 로그인 여부 확인
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // — 여기에 user 모델 추가 —
        userRepository.findById(userId).ifPresent(u -> model.addAttribute("user", u));

        // 2) 폼 초기 데이터 설정
        model.addAttribute("tripRegisterDto", new TripRegisterDto());
        model.addAttribute("allTags", List.of("#혼자서", "#친구랑", "#가족여행", "#연인과", "#휴식", "#관광"));
        return "trips/trip-register";
    }
    
    @PostMapping("/register/trip-register")
    public String processUnifiedForm(
            @ModelAttribute TripRegisterDto dto,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";

        // 필수값 체크
        if (dto.getTitle() == null || dto.getTitle().isBlank()
                || dto.getStartDate() == null || dto.getEndDate() == null
                || dto.getEndDate().isBefore(dto.getStartDate())) {

            model.addAttribute("tripRegisterDto", dto); // 기존 값 유지
            model.addAttribute("allTags", List.of("#혼자서", "#친구랑", "#가족여행", "#연인과", "#휴식", "#관광"));
            model.addAttribute("error", true); // 에러 메시지 출력용
            return "trips/trip-register";     // redirect X
        }

        Trip savedTrip = planService.saveTrip(userId, dto);

        if (dto.getSelectedTags() != null && !dto.getSelectedTags().isEmpty()) {
            planService.saveTags(savedTrip, dto.getSelectedTags());
        }

        return "redirect:/trips/main-past";
    }


}

