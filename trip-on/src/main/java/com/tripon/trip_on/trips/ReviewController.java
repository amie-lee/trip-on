package com.tripon.trip_on.trips;

import com.tripon.trip_on.aws.S3Service;
import com.tripon.trip_on.expenses.ExpenseService;

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
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;

import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripon.trip_on.user.UserService;
import com.tripon.trip_on.user.User;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewLikeService likeService;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final S3Service s3Service;
    private final TripService tripService;
    private final TripTagRepository tripTagRepository;
    private final UserService userService;
    private final ExpenseService expenseService;

    @Value("${upload.dir:${user.home}/uploads}")
    private String uploadDir;

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    // 리전을 ap-southeast-2로 고정
    private final String region = "ap-southeast-2";

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    // ===================== 웹용 후기 페이지 =====================
    /**
     * 여행 후기 페이지(웹)
     * @param tripId 여행 ID
     * @param editReviewId 수정할 후기 ID
     * @param model 뷰에 데이터 전달
     * @return 후기 페이지 뷰 이름
     */
    @GetMapping("/trips/{tripId}/review")
    public String showReviewPage(
            @PathVariable Long tripId,
            @RequestParam(value = "editReviewId", required = false) Long editReviewId,
            Model model,
            HttpSession session) {
        try {
            log.info("Showing review page for tripId: {}", tripId);
            
            // 세션에서 userId 가져오기
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                log.warn("User not logged in, redirecting to login page");
                return "redirect:/user/login";
            }
            
            // 사용자 정보 조회 및 model에 추가
            Optional<User> optUser = userService.getUserById(userId);
            if (optUser.isEmpty()) {
                session.invalidate();
                return "redirect:/user/login";
            }
            // ③ 헤더에 표시할 User 객체를 모델에 추가
            User loginUser = optUser.get();
            model.addAttribute("user", loginUser);
            
            Trip trip = reviewService.getTripPlan(tripId);
            if (trip == null) {
                log.error("Trip not found for id: {}", tripId);
                return "error/404";
            }
            
            log.debug("Trip details - id: {}, title: {}, accommodation: {}", 
                trip.getId(), trip.getTitle(), trip.getAccommodation());
            
            List<Review> reviews = reviewService.getReviews(tripId);
            log.debug("Found {} reviews for tripId: {}", reviews.size(), tripId);
            
            Map<Long, List<ReviewPhoto>> reviewPhotosMap = new HashMap<>();
            for (Review review : reviews) {
                List<ReviewPhoto> photos = reviewPhotoRepository.findByReviewId(review.getId());
                reviewPhotosMap.put(review.getId(), photos);
                log.debug("Review {} has {} photos", review.getId(), photos.size());
            }
            
            TripUpdateDto tripUpdateDto = tripService.getTripUpdateDto(tripId);
            List<TripTag> tags = tripTagRepository.findAllByTripId(tripId);
            log.debug("Found {} tags for tripId: {}", tags.size(), tripId);
            
            int totalExpenseAmount = expenseService.getTotalAmountByTripId(tripId);

            List<String> memberNames = trip.getTripMembers().stream()
            .map(TripMember::getUser)
            .filter(Objects::nonNull)
            .filter(u -> u.getId() != loginUser.getId())
            .map(User::getUsername)
            .collect(Collectors.toList());

            model.addAttribute("currentUserId", userId);
            model.addAttribute("trip", trip);
            model.addAttribute("reviews", reviews);
            model.addAttribute("reviewPhotosMap", reviewPhotosMap);
            model.addAttribute("editReviewId", editReviewId != null ? editReviewId : 0L);
            model.addAttribute("tripUpdateDto", tripUpdateDto);
            model.addAttribute("tags", tags);
            model.addAttribute("tripId", tripId);
            model.addAttribute("totalExpenseAmount", totalExpenseAmount);
            model.addAttribute("memberNames", memberNames);
            
            return "trips/trip-plan-review";
        } catch (Exception e) {
            log.error("Error showing review page: {}", e.getMessage(), e);
            return "error/500";
        }
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
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            HttpSession session) throws IOException {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                log.warn("User not logged in, redirecting to login page");
                return "redirect:/user/login";
            }

            // 최근 1분 이내 동일 내용 중복 저장 방지
            List<Review> recent = reviewService.getReviews(tripId);
            for (Review r : recent) {
                if (r.getUserId().equals(userId) && r.getContent().equals(content)) {
                    if (r.getCreatedAt() != null && java.time.Duration.between(r.getCreatedAt(), java.time.LocalDateTime.now()).toMinutes() < 1) {
                        log.warn("중복 후기 저장 시도 감지: userId={}, tripId={}, content={}", userId, tripId, content);
                        return "redirect:/trips/" + tripId + "/review";
                    }
                }
            }

            log.info("Submit Review - tripId: {}, userId: {}, content: {}", tripId, userId, content);
            log.info("Files count: {}", files != null ? files.length : 0);
            
            // 1. Review 저장
            Review review = reviewService.saveReviewAndReturn(tripId, userId, content);
            if (review == null) {
                log.error("Failed to save review");
                throw new RuntimeException("후기 저장에 실패했습니다.");
            }
            log.info("Review saved successfully with ID: {}", review.getId());

            // 2. 파일 처리
            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        try {
                            log.info("파일 업로드 시도: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
                            String key = s3Service.uploadFile(file);
                            log.info("S3 업로드 성공: {}", key);

                            String fileType = file.getContentType().startsWith("image/") ? "image" : "video";
                            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                                bucket, region, key);

                            reviewService.saveReviewPhoto(review.getId(), imageUrl, imageUrl, fileType);
                            log.info("ReviewPhoto 저장 성공: {} (reviewId: {})", imageUrl, review.getId());
                        } catch (Exception e) {
                            log.error("파일 업로드/DB 저장 실패: {}", e.getMessage(), e);
                            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage());
                        }
                    } else {
                        log.warn("비어있는 파일이 넘어옴: {}", file.getOriginalFilename());
                    }
                }
            } else {
                log.warn("files가 null이거나 비어있음");
            }

            return "redirect:/trips/" + tripId + "/review";
        } catch (Exception e) {
            log.error("Review submission failed: {}", e.getMessage(), e);
            throw new RuntimeException("후기 등록에 실패했습니다: " + e.getMessage());
        }
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
        
        if (files != null) {
            int limit = Math.min(files.length, 10);
            for (int i = 0; i < limit; i++) {
                MultipartFile file = files[i];
                if (!file.isEmpty()) {
                    String fileName = s3Service.uploadFile(file);
                    String fileType = file.getContentType().startsWith("image/") ? "image" : "video";
                    
                    // S3 URL 생성 (us-east-1로 고정)
                    String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileName);
                    
                    reviewService.saveReviewPhoto(review.getId(), imageUrl, imageUrl, fileType);
                }
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
    @PostMapping("/trips/{tripId}/review/{reviewId}/edit")
    @ResponseBody
    public ResponseEntity<Void> updateReview(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            @RequestParam(value = "deletePhotoIds", required = false) String deletePhotoIdsStr,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        // 1. 글 수정
        reviewService.updateReview(reviewId, userId, content);

        // 2. 삭제할 사진 파싱
        List<Long> deletePhotoIds = new ArrayList<>();
        if (deletePhotoIdsStr != null && !deletePhotoIdsStr.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                deletePhotoIds = mapper.readValue(deletePhotoIdsStr, new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                log.error("deletePhotoIds 파싱 실패: {}", e.getMessage());
            }
        }

        // 3. 사진 삭제
        if (deletePhotoIds != null) {
            for (Long photoId : deletePhotoIds) {
                ReviewPhoto photo = reviewPhotoRepository.findById(photoId).orElse(null);
                if (photo != null) {
                    s3Service.deleteFileByUrl(photo.getImageUrl());
                    reviewPhotoRepository.deleteById(photoId);
                }
            }
        }

        // 4. 새로 추가된 파일 저장
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String key = s3Service.uploadFile(file);
                        String fileType = file.getContentType().startsWith("image/") ? "image" : "video";
                        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
                        reviewService.saveReviewPhoto(reviewId, imageUrl, imageUrl, fileType);
                    } catch (Exception e) {
                        throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage());
                    }
                }
            }
        }
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
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
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
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        likeService.like(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/trips/{tripId}/review/{reviewId}/like")
    @ResponseBody
    public ResponseEntity<Void> unlike(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        likeService.unlike(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/trips/{tripId}/review/photo/{photoId}")
    @ResponseBody
    public ResponseEntity<Void> deleteReviewPhoto(
            @PathVariable Long tripId,
            @PathVariable Long photoId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        ReviewPhoto photo = reviewPhotoRepository.findById(photoId)
            .orElseThrow(() -> new RuntimeException("사진을 찾을 수 없습니다."));
        s3Service.deleteFileByUrl(photo.getImageUrl());
        reviewPhotoRepository.deleteById(photoId);
        return ResponseEntity.ok().build();
    }

    private String saveFile(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir).resolve(filename);
        file.transferTo(filePath.toFile());
        return "/uploads/" + filename;
    }
}
