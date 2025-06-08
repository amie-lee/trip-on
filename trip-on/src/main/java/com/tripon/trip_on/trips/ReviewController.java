package com.tripon.trip_on.trips;

import com.tripon.trip_on.service.S3Service;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewLikeService likeService;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final S3Service s3Service;
    private final TripsService tripsService;
    private final TripTagRepository tripTagRepository;

    @Value("${upload.dir:${user.home}/uploads}")
    private String uploadDir;

    @Value("${cloud.aws.s3.bucket}")
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
        // TripUpdateDto, tags 추가 (TripsService, TripTagRepository 활용)
        TripUpdateDto tripUpdateDto = tripsService.getTripUpdateDto(tripId);
        List<TripTag> tags = tripTagRepository.findAllByTripId(tripId);

        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("trip", trip);
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewPhotosMap", reviewPhotosMap);
        model.addAttribute("editReviewId", editReviewId != null ? editReviewId : 0L);
        model.addAttribute("tripUpdateDto", tripUpdateDto);
        model.addAttribute("tags", tags);
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
        try {
            log.info("Submit Review - tripId: {}, userId: {}, content: {}", tripId, userId, content);
            log.info("Files count: {}", files != null ? files.length : 0);
            
            if (userId == null) {
                userId = 0L;
                log.info("userId was null, set to default: {}", userId);
            }
            
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
                            log.info("Processing file: {}, size: {} bytes", 
                                file.getOriginalFilename(), file.getSize());
                            
                            // S3에 파일 업로드
                            String fileName = s3Service.uploadFile(file);
                            log.info("File uploaded to S3: {}", fileName);
                            
                            String fileType = file.getContentType().startsWith("image/") ? "image" : "video";
                            
                            // S3 URL 생성
                            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                                bucket, region, fileName);
                            log.info("Generated S3 URL: {}", imageUrl);
                            
                            // ReviewPhoto DB에 저장
                            reviewService.saveReviewPhoto(review.getId(), imageUrl, imageUrl, fileType);
                            log.info("ReviewPhoto saved for review ID: {}", review.getId());
                        } catch (Exception e) {
                            log.error("File upload failed: {}", e.getMessage(), e);
                            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage());
                        }
                    }
                }
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
    @PutMapping("/trips/{tripId}/review/{reviewId}")
    @ResponseBody
    public ResponseEntity<Void> updateReview(
            @PathVariable Long tripId,
            @PathVariable Long reviewId,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            @RequestParam(value = "deletePhotoIds", required = false) List<Long> deletePhotoIds,
            Principal principal) {
        Long userId = principal != null ? Long.valueOf(principal.getName()) : 0L;
        reviewService.updateReview(reviewId, userId, content);

        // 1. 삭제할 사진 처리
        if (deletePhotoIds != null) {
            for (Long photoId : deletePhotoIds) {
                ReviewPhoto photo = reviewPhotoRepository.findById(photoId)
                    .orElse(null);
                if (photo != null) {
                    s3Service.deleteFileByUrl(photo.getImageUrl());
                    reviewPhotoRepository.deleteById(photoId);
                }
            }
        }

        // 2. 새로 추가된 파일만 처리 (기존 코드)
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        // 1. DB에 사진 정보 임시 저장 (ID 확보)
                        ReviewPhoto photoEntity = ReviewPhoto.builder()
                            .reviewId(reviewId)
                            .imageUrl("임시값")
                            .filePath("임시값")
                            .fileType(file.getContentType().startsWith("image/") ? "image" : "video")
                            .build();
                        reviewPhotoRepository.save(photoEntity);

                        // 2. S3에 파일 업로드 (파일명에 photoEntity.getId() 사용)
                        String fileName = "reviews/" + photoEntity.getId() + "_" + file.getOriginalFilename();
                        s3Service.uploadFile(file, fileName);

                        // 3. S3 URL로 DB 정보 업데이트
                        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileName);
                        photoEntity.setImageUrl(imageUrl);
                        photoEntity.setFilePath(fileName);
                        reviewPhotoRepository.save(photoEntity);
                    } catch (Exception e) {
                        log.error("File upload failed: {}", e.getMessage(), e);
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

    @DeleteMapping("/trips/{tripId}/review/photo/{photoId}")
    @ResponseBody
    public ResponseEntity<Void> deleteReviewPhoto(
            @PathVariable Long tripId,
            @PathVariable Long photoId,
            Principal principal) {
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
