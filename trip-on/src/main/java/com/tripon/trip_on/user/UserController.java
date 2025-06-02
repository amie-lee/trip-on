package com.tripon.trip_on.user;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor

public class UserController {

    @Autowired
    private UserService userService;

    // 회원가입 폼
    @GetMapping("/signup")
    public String signupForm(Model model) {
            // model.addAttribute("signupForm", new User());  // 헤더/이름 바꿈
        model.addAttribute("user", new User());
        model.addAttribute("error", null);
        return "auth/signup";
    }

    // 회원가입 처리 (유효성 검사 포함)
    @PostMapping("/signup")
    public String signup(@ModelAttribute User user,
                     @RequestParam("confirmPassword") String confirmPassword,
                     Model model) {
        String username = user.getUsername();
        String password = user.getPassword();

        boolean nameValid = username.matches("^[a-zA-Z가-힣]+$");
        boolean pwValid = password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

        if (!nameValid) {
            model.addAttribute("usernameError", "이름에는 특수문자나 숫자를 쓸 수 없습니다.");
            return "auth/signup";
        }

        if (!pwValid) {
            model.addAttribute("passwordError", "비밀번호는 영문+숫자 조합 8자 이상이어야 합니다.");
            return "auth/signup";
        }
     
        if (!password.equals(confirmPassword)) {
        model.addAttribute("confirmError", "비밀번호가 일치하지 않습니다.");
        model.addAttribute("user", user);
        return "auth/signup";
    }

        // 이메일 중복 검사
        if (userService.isEmailExists(user.getEmail())) {
            model.addAttribute("emailError", "이미 사용 중인 이메일입니다.");
            model.addAttribute("user", user);
            return "auth/signup";
        }

        userService.register(user);
        return "redirect:/user/login";
    }


    // 로그인 폼 
    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        // 이미 로그인 되어 있으면 마이페이지로
        if (session.getAttribute("userId") != null) {
            return "redirect:/user/mypage";
        }
        return "auth/login";
    }

    // if 문으로 메인페이지(등록된 여행 있음, 없음) 가는 용 로그인 처리 코드 / userId 저장
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
            return hasTrip ? "redirect:/trips/main-past" : "redirect:/trips/main";
        } else {
            model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "auth/login";
        }
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }

 // 1) 비밀번호 재설정 요청 폼
  @GetMapping("/find-password")
  public String showForgotPasswordForm() {
    return "auth/find-password";
  }

  // 2) 이메일로 재설정 링크 발송
  @PostMapping("/find-password")
  public String processForgotPassword(
      @RequestParam String email,
      HttpServletRequest request,
      Model model) {

    String appUrl = request.getScheme()+"://"+
                    request.getServerName()+
                    (request.getServerPort()==80?"":":"+request.getServerPort());
    userService.initiatePasswordReset(email, appUrl);
    model.addAttribute("message",
      "이메일로 비밀번호 재설정 링크를 발송했습니다. 메일이 보이지 않으면 스팸함도 확인해주세요.");
    return "auth/find-password";
  }

  // 3) 링크 클릭 → 새 비밀번호 폼
  @GetMapping("/reset-password")
  public String showResetPasswordForm(
      @RequestParam String token,
      Model model) {

    if (!userService.isPasswordResetTokenValid(token)) {
      model.addAttribute("error", "유효하지 않거나 만료된 링크입니다.");
      return "auth/reset-password";
    }
    model.addAttribute("token", token);
    return "auth/reset-password";
  }

  // 4) 비밀번호 변경 처리
  @PostMapping("/reset-password")
  public String processResetPassword(
      @RequestParam String token,
      @RequestParam String newPassword,
      @RequestParam String confirmPassword,
      Model model) {

    if (!newPassword.equals(confirmPassword)) {
      model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
      model.addAttribute("token", token);
      return "auth/reset-password";
    }
    if (!userService.resetPassword(token, newPassword)) {
      model.addAttribute("error", "유효하지 않거나 만료된 링크입니다.");
      return "auth/reset-password";
    }
    model.addAttribute("message", "비밀번호가 정상적으로 변경되었습니다.");
    return "auth/reset-password";
  }


    // // 마이페이지
    // @GetMapping("/mypage")
    // public String myPage(HttpSession session, Model model) {
    //     Long userId = (Long) session.getAttribute("userId");

    //     // 로그인 안 된 사용자 처리
    //     if (userId == null) {
    //         return "redirect:/user/login"; 
    //     }
        
    //     Optional<User> userOpt = userService.getUserById(userId);

    //     if (userOpt.isPresent()) {
    //         model.addAttribute("user", userOpt.get());
    //         return "users/mypage";
    //     }
    //     return "redirect:/user/login";
    // }
        // 1) 마이페이지
    @GetMapping("/mypage")
    public String myPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }
        Optional<User> opt = userService.getUserById(userId);
        if (opt.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", opt.get());
        // 아래 뷰 경로와 정확히 일치해야 함
        return "users/mypage";
    }


    // // 비밀번호 변경 폼 (GET)
    // @GetMapping("/mypage/password")
    // public String changePasswordForm() {
    //     return "change-password";
    // }

    // 비밀번호 변경 폼 (GET)
    @GetMapping("/mypage/password")
    public String changePasswordForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        // 로그인 안 된 경우 로그인 페이지로 리다이렉트
        if (userId == null) {
            return "redirect:/user/login";
        }

        // header 위해 user 추가가
        Optional<User> opt = userService.getUserById(userId);
        if (opt.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", opt.get());

        return "users/change-password";
    }

    // 비밀번호 변경 처리 (POST)
    @PostMapping("/mypage/password")
    public String changePassword(@RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                HttpSession session,
                                Model model) {
        Long userId = (Long) session.getAttribute("userId");

        // 1) user 조회
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            return "redirect:/user/login";
        }

        User user = userOpt.get();

        // 2) 현재 비밀번호 확인
        if (!user.getPassword().equals(currentPassword)) {
            model.addAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
            return "users/change-password";
        }

        // 3) 현재 비밀번호와 새 비밀번호가 같으면 안 됨
        if (currentPassword.equals(newPassword)) {
            model.addAttribute("error", "새 비밀번호는 현재 비밀번호와 다르게 설정해야 합니다.");
            return "users/change-password";
        }
        
        // 4) 새 비밀번호 유효성 검사
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
            model.addAttribute("error", "새 비밀번호는 영문+숫자 포함 8자 이상이어야 합니다.");
            return "users/change-password";
        }

        // 5) 새 비밀번호와 확인 비밀번호 일치 검사
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
            return "users/change-password";
        }

        // 6) 비밀번호 변경 성공
        boolean changed = userService.changePassword(userId, newPassword);
        if (!changed) {
            model.addAttribute("error", "비밀번호 변경 중 오류가 발생했습니다.");
            return "users/change-password";
        }

        model.addAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
        return "users/change-password";
    }


    // // 탈퇴 폼 보여주기기
    // @GetMapping("/mypage/delete")
    // public String showDeleteForm(HttpSession session) {
    //     if (session.getAttribute("userId") == null) {
    //         return "redirect:/user/login";
    //     }
    //     return "users/delete";  
    // }    

    // // 회원 탈퇴 처리
    // @PostMapping("/mypage/delete")
    // public String deleteAccount(HttpSession session) {
    //     Long userId = (Long) session.getAttribute("userId");
    //     userService.deleteUser(userId);
    //     session.invalidate();
    //     return "redirect:/user/login";
    // }
        // 4) 실제 탈퇴 처리
    // @PostMapping("/mypage/delete")
    // public String deleteAccount(HttpSession session) {
    //     Long userId = (Long) session.getAttribute("userId");
    //     if (userId != null) {
    //         userService.deleteUser(userId);
    //         session.invalidate();
    //     }
    //     return "redirect:/user/login";
    // }

        // 1) 회원 탈퇴 폼 보여주기 (GET /user/mypage/delete)
    @GetMapping("/mypage/delete")
    public String showDeleteForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        // 헤더에서 쓰기 위해 user 추가
        Optional<User> opt = userService.getUserById(userId);
        if (opt.isEmpty()) {
            session.invalidate();
            return "redirect:/user/login";
        }
        model.addAttribute("user", opt.get());

        return "users/delete";
    }

    // 2) 회원 탈퇴 처리(POST) 
    @PostMapping("/mypage/delete")
    public String deleteAccount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            userService.deleteUser(userId);
            session.invalidate();
        }
        return "redirect:/user/login";
    }

    /**  
     * header에 로고(Homepage) 클릭용. 
     * 세션에 userId 가 없으면 로그인 페이지로, 
     * 있으면 hasTrip 여부에 따라 main-past 또는 main 으로 보냄. 
     */
    @GetMapping("/")       // 혹은 @GetMapping("") 로 루트 URI 매핑
    public String homeRedirect(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }
        boolean hasTrip = userService.hasTrips(userId);
        return hasTrip 
            ? "redirect:/trips/main-past" 
            : "redirect:/trips/main";
    }

}
