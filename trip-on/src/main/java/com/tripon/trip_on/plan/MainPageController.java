package com.tripon.trip_on.plan;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MainPageController
 * - 로그인 후 진입하는 메인페이지 컨트롤러
 * - 사용자 ID로 해당 사용자의 여행 목록을 불러옴
 * - 오늘 날짜 기준으로 예정된 여행 / 지난 여행을 구분해서 출력
 * - 여행별로 태그도 함께 조회하여 Trip 객체에 저장 후 View로 전달
 */
@Controller
public class MainPageController {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripTagRepository tripTagRepository;

    /**
     * /mainpage
     * - 등록된 여행이 있을 경우 진입
     * - 여행을 today 기준으로 분류하고, 태그도 각 리스트에 넣어 model로 전달
     */
    @GetMapping("/mainpage")
    public String mainPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        List<Trip> trips = tripRepository.findByCreatorId(userId);

        LocalDate today = LocalDate.now();

        // 오늘 이후 또는 오늘 시작되는 여행 → 예정된 여행
        List<Trip> upcomingTrips = trips.stream()
            .filter(trip -> !trip.getStartDate().isBefore(today))
            .collect(Collectors.toList());

        // 이미 종료된 여행 → 지난 여행
        List<Trip> pastTrips = trips.stream()
            .filter(trip -> trip.getEndDate().isBefore(today))
            .collect(Collectors.toList());

        // 예정된 여행 리스트에 태그 주입
        for (Trip trip : upcomingTrips) {
            List<String> tagNames = tripTagRepository.findByTripId(trip.getId())
                .stream()
                .map(TripTag::getTagName)
                .collect(Collectors.toList());
            trip.setTags(tagNames);
        }

        // 지난 여행 리스트에 태그 주입
        for (Trip trip : pastTrips) {
            List<String> tagNames = tripTagRepository.findByTripId(trip.getId())
                .stream()
                .map(TripTag::getTagName)
                .collect(Collectors.toList());
            trip.setTags(tagNames);
        }

        // 모델에 담아 Thymeleaf로 전달
        model.addAttribute("upcomingTrips", upcomingTrips);
        model.addAttribute("pastTrips", pastTrips);
        return "mainpage";
    }

    /**
     * /mainpage-new
     * - 등록된 여행이 하나도 없을 경우 진입하는 페이지
     */
    @GetMapping("/mainpage-new")
    public String mainPageNew() {
        return "mainpage-new";
    }
}
