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
import java.util.Map;
import java.util.HashMap;

import lombok.RequiredArgsConstructor;
import java.security.Principal;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewLikeService likeService;
    private final ReviewPhotoRepository reviewPhotoRepository;

    @Value("${upload.dir:${user.home}/uploads}")
    private String uploadDir;

    // ===================== 웹용 후기 페이지 =====================
    /**
     * 여행 후기 페이지(웹)
     * @param tripId 여행 ID
     * @param editReviewId 수정할 후기 ID
     * @param model 뷰에 데이터 전달
     * @return 후기 페이지 뷰 이름
     */
    @GetMapping("/trips/{tripId}/review")
    public String showReviewPage(@PathVariable Long tripId,
                                 @RequestParam(value = "editReviewId", required = false) Long editReviewId,
                                 Model model, Principal principal) {
        Object trip = reviewService.getTripPlan(tripId);
        if (trip == null) {
            return "error/404"; // 여행이 없으면 404 페이지로 이동
        }
        
        Long currentUserId = 0L;
        if (principal != null) {
            try {
                currentUserId = Long.valueOf(principal.getName());
            } catch (Exception e) {
                currentUserId = 0L;
            }
            model.addAttribute("user", principal);
        }
        List<Review> reviews = reviewService.getReviews(tripId);
        // 각 후기별 첨부파일 리스트 맵 생성
        Map<Long, List<ReviewPhoto>> reviewPhotosMap = new HashMap<>();
        for (Review review : reviews) {
            List<ReviewPhoto> photos = reviewPhotoRepository.findByReviewId(review.getId());
            reviewPhotosMap.put(review.getId(), photos);
        }
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("trip", trip);
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewPhotosMap", reviewPhotosMap);
        model.addAttribute("editReviewId", editReviewId != null ? editReviewId : 0L);
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
            @RequestParam(value = "file", required = false) MultipartFile[] files
    ) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        if (userId == null) userId = 0L;
        
        // 1. 먼저 Review 저장
        Review review = reviewService.saveReviewAndReturn(tripId, userId, content);

        // 2. 파일이 있으면 저장하고 ReviewPhoto DB에 저장
        if (files != null) {
            int limit = Math.min(files.length, 10);
            for (int i = 0; i < limit; i++) {
                MultipartFile file = files[i];
                if (!file.isEmpty()) {
                    String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(filename);
                    file.transferTo(filePath.toFile());
                    
                    String imageUrl = "/uploads/" + filename;
                    String fileType = file.getContentType().startsWith("image/") ? "image" : "video";
                    
                    // ReviewPhoto DB에 저장 (file_path 포함)
                    reviewService.saveReviewPhoto(review.getId(), imageUrl, filePath.toString(), fileType);
                }
            }
        }

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
            @RequestParam(value = "file", required = false) MultipartFile[] files
    ) throws IOException {
        if (userId == null) userId = 0L;
        Review review = reviewService.saveReviewAndReturn(tripId, userId, content);
        // 파일 10개까지만 저장, 초과분은 무시
        if (files != null) {
            int limit = Math.min(files.length, 10);
            for (int i = 0; i < limit; i++) {
                MultipartFile file = files[i];
                String imageUrl = saveFile(file);
                String fileType = file.getContentType().startsWith("image/") ? "image" : "video";
                reviewService.saveReviewPhoto(review.getId(), imageUrl, fileType);
            }
        }
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
    @PutMapping("/trips/{tripId}/review/{reviewId}")
    @ResponseBody
    public ResponseEntity<Void> updateReview(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        Long userId = principal != null ? Long.valueOf(principal.getName()) : 0L;
        String content = (String) body.get("content");
        reviewService.updateReview(reviewId, userId, content);
        return ResponseEntity.ok().build();
    }

    /**
     * 후기 삭제(REST)
     */
    @DeleteMapping("/trips/{tripId}/review/{reviewId}")
    @ResponseBody
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            Principal principal) {
        Long userId = principal != null ? Long.valueOf(principal.getName()) : 0L;
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

    @PostMapping("/trips/{tripId}/review/{reviewId}/like")
    @ResponseBody
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            Principal principal) {
        Long userId = principal != null ? Long.valueOf(principal.getName()) : 0L;
        likeService.like(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/trips/{tripId}/review/{reviewId}/like")
    @ResponseBody
    public ResponseEntity<Void> unlike(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            Principal principal) {
        Long userId = principal != null ? Long.valueOf(principal.getName()) : 0L;
        likeService.unlike(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    private String saveFile(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir).resolve(filename);
        file.transferTo(filePath.toFile());
        return "/uploads/" + filename;
    }
}
