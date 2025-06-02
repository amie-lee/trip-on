package com.tripon.trip_on.trips;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

import lombok.RequiredArgsConstructor;
import java.security.Principal;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewLikeService likeService;

    @Value("${upload.dir:${user.home}/uploads}")
    private String uploadDir;

    // ===================== 웹용 후기 페이지 =====================
    /**
     * 여행 후기 페이지(웹)
     * @param tripId 여행 ID
     * @param model 뷰에 데이터 전달
     * @return 후기 페이지 뷰 이름
     */
    @GetMapping("/trips/{tripId}/review")
    public String showReviewPage(@PathVariable Long tripId, Model model) {
        Object trip = reviewService.getTripPlan(tripId);
        if (trip == null) {
            return "error/404"; // 여행이 없으면 404 페이지로 이동
        }
        model.addAttribute("trip", trip);
        model.addAttribute("reviews", reviewService.getReviews(tripId));
        return "trips/trip-plan-review";
    }

    /**
     * 여행 후기 등록(웹)
     * @param tripId 여행 ID
     * @param userId 작성자 ID(비회원은 0)
     * @param content 후기 내용
     * @param files 첨부파일 목록
     * @return 후기 페이지로 리다이렉트
     */
    @PostMapping("/trips/{tripId}/review")
    public String submitReview(
            @PathVariable Long tripId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam("content") String content,
            @RequestParam(value = "reviewFile", required = false) MultipartFile[] files
    ) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
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
                    savedPaths.add("/uploads/" + filename);
                }
            }
        }

        if (userId == null) userId = 0L;
        reviewService.saveReview(tripId, userId, content, savedPaths);
        return "redirect:/trips/" + tripId + "/review";
    }

    // ===================== REST API =====================
    /**
     * 후기 등록(REST)
     */
    @PostMapping("/api/trips/{tripId}/reviews")
    @ResponseBody
    public ResponseEntity<Void> createReviewApi(
            @PathVariable Long tripId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam String content,
            @RequestParam(required = false) List<String> filePaths
    ) {
        if (userId == null) userId = 0L;
        reviewService.saveReview(tripId, userId, content, filePaths);
        return ResponseEntity.ok().build();
    }

    /**
     * 후기 목록 조회(REST)
     */
    @GetMapping("/api/trips/{tripId}/reviews")
    @ResponseBody
    public ResponseEntity<List<Review>> getReviewsApi(@PathVariable Long tripId) {
        return ResponseEntity.ok(reviewService.getReviews(tripId));
    }

    /**
     * 후기 수정(REST)
     */
    @PutMapping("/api/reviews/{reviewId}")
    @ResponseBody
    public ResponseEntity<Void> updateReviewApi(
            @PathVariable Long reviewId,
            @RequestParam String content,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        if (userId == null) userId = 0L;
        reviewService.updateReview(reviewId, userId, content);
        return ResponseEntity.ok().build();
    }

    /**
     * 후기 삭제(REST)
     */
    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseBody
    public ResponseEntity<Void> deleteReviewApi(
            @PathVariable Long reviewId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        if (userId == null) userId = 0L;
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    // ===================== 좋아요 관련 API =====================
    /**
     * 후기 좋아요 등록(REST)
     * Principal: 인증된 사용자 정보
     */
    @PostMapping("/api/reviews/{reviewId}/likes")
    public ResponseEntity<Void> likeReviewApi(@PathVariable Long reviewId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        likeService.like(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 후기 좋아요 취소(REST)
     */
    @DeleteMapping("/api/reviews/{reviewId}/likes")
    public ResponseEntity<Void> unlikeReviewApi(@PathVariable Long reviewId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        likeService.unlike(reviewId, userId);
        return ResponseEntity.ok().build();
    }
}
