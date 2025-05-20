package com.tripon.trip_on.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// User 테이블에 접근하는 인터페이스
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
}
