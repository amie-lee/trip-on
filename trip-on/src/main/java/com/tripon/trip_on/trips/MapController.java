package com.tripon.trip_on.trips;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapController {

    // 검색 팝업
    @GetMapping("/map-popup")
    public String showMapPopup() {
        return "trips/map-popup";
    }


}
