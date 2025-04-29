package com.tripon.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// User 테이블에 접근하는 인터페이스
public interface UserRepository extends JpaRepository<User, Long> {
    // username으로 사용자 찾기
    Optional<User> findByUsername(String username);

    // email로 사용자 찾기
    Optional<User> findByEmail(String email);
}
