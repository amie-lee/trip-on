package com.tripon.trip_on.trips;

import com.tripon.trip_on.trips.ReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tripReview")
public class ReviewController {
    private final ReviewService reviewService;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{tripId}/review")
    public String showReviewPage(@PathVariable Long tripId, Model model) {
        model.addAttribute("trip", reviewService.getTripPlan(tripId));
        model.addAttribute("reviews", reviewService.getReviews(tripId));
        return "trips/4_trip-plan-review";
    }

    @PostMapping("/{tripId}/review")
    public String submitReview(
            @PathVariable Long tripId,
            @RequestParam("content") String content,
            @RequestParam(value = "reviewFile", required = false) MultipartFile[] files
    ) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<String> savedPaths = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(filename);
                    file.transferTo(filePath.toFile());
                    savedPaths.add("/" + uploadDir + "/" + filename);
                }
            }
        }

        reviewService.saveReview(tripId, content, savedPaths);
        return "redirect:/trips/" + tripId + "/review";
    }
}
