package com.tripon.trip_on.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tripon.trip_on.aws.S3Service;
import com.tripon.trip_on.trips.TripRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final S3Service s3Service;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired private JavaMailSender mailSender;    // spring-boot-starter-mail 의존 필요
    
    @Value("${spring.mail.username}")
    private String fromAddress;

    // 이메일 중복 체크
    public boolean isEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    // 회원가입 저장
    public User register(User user) {
        if (user.getProfileImage() == null || user.getProfileImage().isEmpty()) {
            user.setProfileImage("/images/tripon_profile_demo.png"); // 기본 프로필 경로
        }
        return userRepository.save(user);
    }


    // 로그인 처리 (이메일+비밀번호 확인)
    public Optional<User> login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }


       //  * 1) 이메일로 회원 찾기, 2) 토큰 생성▶DB 저장, 3) 이메일 발송

    public void initiatePasswordReset(String email, String appUrl) {
        logger.debug("→ initiatePasswordReset 호출, email={}", email);

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.warn("→ 등록된 이메일 아님, email={}", email);
            return;
        }
        var user = userOpt.get();

        // 1) 토큰 생성·저장
        String token = UUID.randomUUID().toString(); // 랜덤
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 1시간 후 만료
        userRepository.save(user);
        logger.debug("→ 토큰 생성/저장 완료, token={}, expiry={}", token, user.getResetTokenExpiry());

        // 2) 메일 발송
        String resetLink = appUrl + "/user/reset-password?token=" + token;
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(user.getEmail());
        mail.setSubject("TripOn 비밀번호 재설정 링크");
        mail.setText("안녕하세요, " + user.getUsername() + "님.\n\n"
                   + "다음 링크(1시간 유효)를 클릭하세요:\n" + resetLink);

        try {
            mailSender.send(mail);
            logger.info("⇒ 비밀번호 재설정 메일 전송 성공: {}", user.getEmail());
        } catch (MailException ex) {
            logger.error("⇒ 메일 전송 실패: {}", ex.getMessage(), ex);
        }
    }



    /** 2) 토큰 유효성 검사 */
    public boolean isPasswordResetTokenValid(String token) {
        return userRepository.findByResetToken(token)
        .filter(u ->
            u.getResetTokenExpiry() != null &&
            u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
        .isPresent();
    }

    /** 3) 토큰으로 실제 비밀번호 갱신 */
    public boolean resetPassword(String token, String newPassword) {
        var opt = userRepository.findByResetToken(token)
        .filter(u -> u.getResetTokenExpiry().isAfter(LocalDateTime.now()));
        if (opt.isEmpty()) return false;
        User user = opt.get();
        user.setPassword(newPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return true;
    }
    
    // 해당 사용자가 등록한 여행이 존재하는지 여부 확인
    public boolean hasTrips(Long userId) {
        return tripRepository.existsByCreatorId(userId);
    }

    // userId로 User 객체 조회 (마이페이지 등에서 사용 가능)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 비밀번호 변경
    @Transactional
    public boolean changePassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // 회원 탈퇴
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    //  /**
    //  * 사용자가 업로드한 프로필 이미지를 S3에 저장하고, 해당 URL을 User 엔티티에 반영한 뒤 저장한다.
    //  * @param userId  프로필을 변경할 사용자 ID
    //  * @param file    업로드된 MultipartFile (이미지)
    //  * @return 변경된 User 객체(Optional). userId가 없거나 예외 발생 시 Optional.empty() 반환.
    //  */
    // public Optional<User> updateProfileImage(Long userId, MultipartFile file) {
    //     Optional<User> userOpt = userRepository.findById(userId);
    //     if (userOpt.isEmpty()) {
    //         return Optional.empty();
    //     }

    //     User user = userOpt.get();
    //     try {
    //         // S3에 업로드 (디렉토리: "profiles")
    //         String imageUrl = s3Service.uploadFile(file, "profiles");
            
    //         // 만약 기존에 기본 이미지가 아닌 커스텀 이미지가 있었고, 삭제하고 싶다면:
    //         // String oldImageKey = extractKeyFromUrl(user.getProfileImage());
    //         // s3Service.deleteFile(oldImageKey);

    //         user.setProfileImage(imageUrl);
    //         userRepository.save(user);
    //         return Optional.of(user);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //         return Optional.empty();
    //     }
    // }

    // // (선택) URL에서 S3 키를 추출하는 유틸 메서드. 
    // // S3 URL 형식이 "https://bucket-name.s3.region.amazonaws.com/key" 일 때 key 부분만 뽑는다.
    // private String extractKeyFromUrl(String url) {
    //     // 예: https://my-bucket.s3.ap-northeast-2.amazonaws.com/profiles/uuid.png
    //     // -> "profiles/uuid.png" 만 추출
    //     if (url == null) return "";
    //     int idx = url.indexOf(".amazonaws.com/");
    //     if (idx < 0) return "";
    //     return url.substring(idx + ".amazonaws.com/".length());
    // }

    //     public Optional<User> updateProfileImage(Long userId, MultipartFile file) throws IOException {
    //     Optional<User> userOpt = userRepository.findById(userId);
    //     if (userOpt.isEmpty()) {
    //         return Optional.empty();
    //     }

    //     User user = userOpt.get();

    //     // S3Service를 호출해서 업로드하고, 퍼블릭 URL을 받아온다.
    //     String imageUrl = s3Service.uploadFile(file, "profiles");
    //     user.setProfileImage(imageUrl);
    //     userRepository.save(user);
    //     return Optional.of(user);
    // }

        @Transactional
    public Optional<User> updateProfileImage(Long userId, MultipartFile file) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        try {
            // S3에 업로드
            String imageUrl = s3Service.uploadFile(file, "profiles");
            user.setProfileImage(imageUrl);
            userRepository.save(user);
            return Optional.of(user);

        } catch (Exception e) {
            // 예외가 발생했을 때 반드시 로그를 남깁니다.
            logger.error("[UserService.updateProfileImage] 예외 발생", e);
            return Optional.empty();
        }
    }
}
