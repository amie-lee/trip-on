package com.tripon.trip_on.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tripon.trip_on.plan.TripRepository;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TripRepository tripRepository;

    // 회원가입 처리
    public User register(User user) {
        return userRepository.save(user);
    }

    // 로그인 처리 (이메일 + 비밀번호 확인)
    public Optional<User> login(String email, String password) { 
        Optional<User> userOptional = userRepository.findByEmail(email); // 이메일로 사용자 검색
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) { // 비밀번호 일치 확인
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // 이메일로 비밀번호 찾기
    public Optional<String> findPasswordByEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        return userOptional.map(User::getPassword);
    }

    // 해당 사용자가 등록한 여행이 존재하는지 여부 확인
    public boolean hasTrips(Long userId) {
        return tripRepository.existsByCreatorId(userId);
    }
}
