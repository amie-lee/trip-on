package com.tripon.trip_on.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 회원가입 폼
    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@ModelAttribute User user) {
        userService.register(user);
        return "redirect:/user/login";
        // return "login";

    }

    // 로그인 폼
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }


    //if 문으로 메인페이지(등록된 여행 있음, 없음) 가는 용 로그인 처리 코드
    @PostMapping("/login")
    public String login(@RequestParam String email,
        @RequestParam String password,
        HttpSession session,
        Model model) {
        var userOptional = userService.login(email, password);
        System.out.println("[DEBUG] login attempt: " + email + " / result: " + userOptional.isPresent());
        if (userOptional.isPresent()) {
            var user = userOptional.get();
            session.setAttribute("userId", user.getId());
            boolean hasTrip = userService.hasTrips(user.getId());
            System.out.println("[DEBUG] hasTrip = " + hasTrip);
            return hasTrip ? "redirect:/mainpage" : "redirect:/mainpage-new";
        }else {
        model.addAttribute("error", "Invalid email or password");
        return "login";
    }
    }

    // 비밀번호 찾기 폼
    @GetMapping("/find-password")
    public String findPasswordForm() {
        return "find-password";
    }

    // 비밀번호 찾기 처리
    @PostMapping("/find-password")
    public String findPassword(@RequestParam String email, Model model) {
        var passwordOptional = userService.findPasswordByEmail(email);
        if (passwordOptional.isPresent()) {
            model.addAttribute("message", "비밀번호는: " + passwordOptional.get());
        } else {
            model.addAttribute("error", "이메일이 존재하지 않습니다.");
        }
        return "find-password";
    }
}
