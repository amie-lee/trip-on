package com.tripon.trip_on.trips;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MainPageController {

    @Autowired
    private TripsService planService;

    @GetMapping({"/", "/trips/main-past"})
public String mainPage(HttpSession session, Model model) {
    Long userId = (Long) session.getAttribute("userId");
    if (userId == null) {
        return "redirect:/user/login";
    }

    LocalDate today = LocalDate.now();
    List<Trip> allTrips = planService.findByCreatorId(userId);

    // 오늘 이후 종료되는 여행(진행 중 + 예정된 여행)을 upcomingTrips로 포함
    List<Trip> upcomingTrips = allTrips.stream()
        .filter(trip -> !trip.getEndDate().isBefore(today))
        .collect(Collectors.toList());

    // 오늘 이전에 종료된 여행만 pastTrips로 포함
    List<Trip> pastTrips = allTrips.stream()
        .filter(trip -> trip.getEndDate().isBefore(today))
        .collect(Collectors.toList());

    upcomingTrips.forEach(trip -> trip.setTags(planService.getTags(trip.getId())));
    pastTrips    .forEach(trip -> trip.setTags(planService.getTags(trip.getId())));

    model.addAttribute("upcomingTrips", upcomingTrips);
    model.addAttribute("pastTrips", pastTrips);
    return "trips/main-past";
}

    @GetMapping("/trips/main")
    public String mainPageNew() {
        return "trips/main";
    }
}
