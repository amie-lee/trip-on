package com.tripon.trip_on.trips;

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

    /**
     * 여행 상세 후기 페이지로 이동
     */
    @GetMapping("/{tripId}/review")
    public String showReviewPage(@PathVariable Long tripId, Model model) {
        model.addAttribute("trip", reviewService.getTripPlan(tripId));
        model.addAttribute("reviews", reviewService.getReviews(tripId));
        return "trips/4.trip-plan-review";
    }

    /**
     * DTO를 컨트롤러 내부에서 정의하고 바인딩하여 후기 등록 처리
     */
    @PostMapping("/{tripId}/review")
    public String submitReview(@ModelAttribute ReviewDto dto) throws IOException {
        // 파일 업로드 디렉터리 준비
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 파일 저장 및 경로 수집
        List<String> savedPaths = new ArrayList<>();
        MultipartFile[] files = dto.getReviewFile();
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
        dto.setFilePaths(savedPaths);

        // 서비스 호출 (DTO에서 직접 값 사용)
        reviewService.saveReview(dto.getTripId(), dto.getContent(), dto.getFilePaths());
        return "redirect:/trips/" + dto.getTripId() + "/review";
    }

    /**
     * 후기를 위한 DTO를 컨트롤러 내부에 정의
     */
    public static class ReviewDto {
        private Long tripId;
        private String content;
        private MultipartFile[] reviewFile;
        private List<String> filePaths;

        public Long getTripId() {
            return tripId;
        }
        public void setTripId(Long tripId) {
            this.tripId = tripId;
        }
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public MultipartFile[] getReviewFile() {
            return reviewFile;
        }
        public void setReviewFile(MultipartFile[] reviewFile) {
            this.reviewFile = reviewFile;
        }
        public List<String> getFilePaths() {
            return filePaths;
        }
        public void setFilePaths(List<String> filePaths) {
            this.filePaths = filePaths;
        }
    }

    public interface ReviewService {
    /**
     * 특정 여행 계획 정보 조회
     */
    Object getTripPlan(Long tripId);

    /**
     * 특정 여행에 대한 후기 목록 조회
     */
    List<Review> getReviews(Long tripId);

    /**
     * 후기 저장
     */
    void saveReview(Long tripId, String content, List<String> filePaths);
}
}
