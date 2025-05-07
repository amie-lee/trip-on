package com.tripon.trip_on.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user") //이걸 뺄까요? 이거 넣으면 /user/login 이렇게 구분돼서 좋긴 한데 괜히 필요없는 거 같기도 해서서
public class UserController {
 
    @Autowired
    private UserService userService;

    // 회원가입 폼
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("error", null); // 초기 진입 시에는 에러 없음
        return "signup";
    }
    

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@ModelAttribute User user, Model model) {
        String username = user.getUsername();
        String password = user.getPassword();
    
        boolean nameValid = username.matches("^[a-zA-Z가-힣]+$");
        boolean pwValid = password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    
        if (!nameValid) {
            model.addAttribute("error", "이름에는 특수문자나 숫자를 쓸 수 없습니다.");
            return "signup";
        }
    
        if (!pwValid) {
            model.addAttribute("error", "비밀번호는 영문+숫자 조합 8자 이상이어야 합니다.");
            return "signup";
        }
    
        userService.register(user);
        return "redirect:/user/login";
    }
    


    // // 회원가입 처리
    // @PostMapping("/signup")
    // public String signup(@ModelAttribute User user) {
    //     userService.register(user);
    //     return "redirect:/user/login";
    //     // return "login";

    // }

    // 로그인 폼
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    // 로그인 처리
    @PostMapping("/login")
    public String login(@RequestParam String email,@RequestParam String password,
                        HttpSession session,Model model) {
        var userOptional = userService.login(email, password);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            session.setAttribute("user", userOptional.get());
            model.addAttribute("username", user.getUsername());
            return "login-success";
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        } 
    }

    // 로그아웃 처리
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 비우기
        // return "redirect:/"; //나중에 홈 화면으로 연결되게 하기
        return "login";
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
