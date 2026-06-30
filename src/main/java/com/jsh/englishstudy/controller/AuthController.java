package com.jsh.englishstudy.controller;

import com.jsh.englishstudy.entity.User; // ✅ 패키지 수정
import com.jsh.englishstudy.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 회원가입 페이지
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // 회원가입 처리
    @PostMapping("/api/auth/register")
    @ResponseBody
    public String register(@RequestParam String nickname,
                           @RequestParam String password,
                           HttpSession session) {

        // 닉네임 중복 체크
        if (userRepository.findByNickname(nickname).isPresent()) {
            return "DUPLICATE_NICKNAME";
        }

        // 신규 유저 생성
        User newUser = new User();
        newUser.setNickname(nickname);
        newUser.setPassword(password);
        newUser.setRegistered(true); // ✅ NOT NULL 컬럼 반드시 세팅

        User savedUser = userRepository.save(newUser);

        // 가입 즉시 로그인 처리
        session.setAttribute("loginUser", savedUser);

        return "SUCCESS";
    }

    // 로그인 처리
    @PostMapping("/api/auth/login")
    @ResponseBody
    public String login(@RequestParam String nickname,
                        @RequestParam String password,
                        @RequestParam(required = false, defaultValue = "false") boolean rememberMe,
                        HttpSession session,
                        HttpServletResponse response) {

        User user = userRepository.findByNickname(nickname).orElse(null);

        if (user == null || !user.getPassword().equals(password)) {
            return "FAIL";
        }

        session.setAttribute("loginUser", user);

        // 자동 로그인 쿠키
        if (rememberMe) {
            Cookie cookie = new Cookie("remember_me", user.getNickname());
            cookie.setMaxAge(60 * 60 * 24 * 30);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        return "SUCCESS";
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();

        Cookie cookie = new Cookie("remember_me", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/login";
    }
}