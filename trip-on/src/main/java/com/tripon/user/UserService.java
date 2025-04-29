package com.tripon.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 회원가입
    public User register(User user) {
        return userRepository.save(user);
    }

    // 로그인 (username + password 검사)
    public Optional<User> login(String email, String password) { 
        Optional<User> userOptional = userRepository.findByEmail(email); //email로 유저 찾아서
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) { //password가 일치하는지 확인인
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
}
