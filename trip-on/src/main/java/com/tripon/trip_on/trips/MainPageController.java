// package com.tripon.trip_on.trips;

// import jakarta.servlet.http.HttpSession;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;

// import java.time.LocalDate;
// import java.util.List;
// import java.util.stream.Collectors;

// @Controller
// public class MainPageController {

//     @Autowired
//     private TripsService planService;

//     @GetMapping({"/", "/trips/main-past"})
// public String mainPage(HttpSession session, Model model) {
//     Long userId = (Long) session.getAttribute("userId");
//     if (userId == null) {
//         return "redirect:/user/login";
//     }

//     LocalDate today = LocalDate.now();
//     List<Trip> allTrips = planService.findByCreatorId(userId);

//     // 오늘 이후 종료되는 여행(진행 중 + 예정된 여행)을 upcomingTrips로 포함
//     List<Trip> upcomingTrips = allTrips.stream()
//         .filter(trip -> !trip.getEndDate().isBefore(today))
//         .collect(Collectors.toList());

//     // 오늘 이전에 종료된 여행만 pastTrips로 포함
//     List<Trip> pastTrips = allTrips.stream()
//         .filter(trip -> trip.getEndDate().isBefore(today))
//         .collect(Collectors.toList());

//     upcomingTrips.forEach(trip -> trip.setTags(planService.getTags(trip.getId())));
//     pastTrips    .forEach(trip -> trip.setTags(planService.getTags(trip.getId())));

//     model.addAttribute("upcomingTrips", upcomingTrips);
//     model.addAttribute("pastTrips", pastTrips);
//     return "trips/main-past";
// }

//     @GetMapping("/trips/main")
//     public String mainPageNew() {
//         return "trips/main";
//     }
// }


package com.tripon.trip_on.trips;

import com.tripon.trip_on.user.User;
import com.tripon.trip_on.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class MainPageController {

    @Autowired
    private TripService planService;

    @Autowired
    private UserService userService;

    /**
     * "/" 또는 "/trips/main-past" 요청 시 호출
     * 1) 세션에서 userId 확인 → 없으면 로그인 페이지로 리다이렉트
     * 2) userId로 User 객체 조회 → 모델에 추가(model.addAttribute("user", …))
     * 3) 여행 목록(upcomingTrips, pastTrips)도 모델에 추가
     * 4) "trips/main-past" 뷰 렌더링
     */
    @GetMapping({"/", "/trips/main-past"})
    public String mainPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ── (A) User 객체 조회 및 모델에 추가 ──
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", optUser.get());

        // ── (B) 여행 목록 계산 ──
        LocalDate today = LocalDate.now();
        List<Trip> allTrips = planService.findByCreatorId(userId);

        // 예정 중인 여행 (endDate >= today)
        List<Trip> upcomingTrips = allTrips.stream()
            .filter(trip -> !trip.getEndDate().isBefore(today))
            .collect(Collectors.toList());
        // 과거 여행 (endDate < today)
        List<Trip> pastTrips = allTrips.stream()
            .filter(trip -> trip.getEndDate().isBefore(today))
            .collect(Collectors.toList());

        upcomingTrips.forEach(trip -> trip.setTags(planService.getTags(trip.getId())));
        pastTrips    .forEach(trip -> trip.setTags(planService.getTags(trip.getId())));

        model.addAttribute("upcomingTrips", upcomingTrips);
        model.addAttribute("pastTrips", pastTrips);

        return "trips/main-past";
    }

    /**
     * "/trips/main" 요청 시 호출
     * 1) 세션에서 userId 확인 → 없으면 로그인 페이지로 리다이렉트
     * 2) userId로 User 객체 조회 → 모델에 추가(model.addAttribute("user", …))
     * 3) "trips/main" 뷰 렌더링
     */
    @GetMapping("/trips/main")
    public String mainPageNew(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // ── User 조회 및 모델에 추가 ──
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", optUser.get());

        // (만약 나중에, 여행이 하나도 없는 경우를 위한 로직을 추가)
        return "trips/main";
    }
}

