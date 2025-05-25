package com.tripon.trip_on.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.tripon.trip_on.plan.TripRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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
}
