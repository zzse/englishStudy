package com.jsh.englishstudy.controller;

import com.jsh.englishstudy.entity.User;
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

    // 로그인 페이지 진입
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 회원가입 페이지 진입
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // 1. 누구나 가능한 회원가입 처리 로직 (가입 즉시 세션 강제 발급형으로 업그레이드)
    @PostMapping("/api/auth/register")
    @ResponseBody
    public String register(@RequestParam String nickname,
                           @RequestParam String password,
                           HttpSession session) { // 🚀 자동 로그인을 위해 세션 객체를 파라미터에 추가합니다.

        // 이미 해당 닉네임으로 가입한 사람이 있는지 검사
        if (userRepository.findByNickname(nickname).isPresent()) {
            return "DUPLICATE_NICKNAME";
        }

        // 중복이 없다면 즉시 새로운 유저 객체 생성 및 저장
        User newUser = new User();
        newUser.setNickname(nickname);
        newUser.setPassword(password); // (스터디용 단순 프로젝트이므로 평문 처리)
        newUser.setRegistered(true);

        User savedUser = userRepository.save(newUser);

        // 🚨 [오늘의 핵심 무적 치트키]
        // joy로 가입하자마자 메인 화면에서 '스터디원님'이 아니라 진짜 'joy님'이 뜨도록
        // 가입 성공 데이터베이스 트랜잭션이 끝나는 즉시 세션 인증 도장을 강제로 콱 찍어버립니다!
        session.setAttribute("loginUser", savedUser);

        return "SUCCESS";
    }

    // 2. 로그인 처리 로직 (자동 로그인 쿠키 포함)
    @PostMapping("/api/auth/login")
    @ResponseBody
    public String login(@RequestParam String nickname,
                        @RequestParam String password,
                        @RequestParam(required = false) boolean rememberMe,
                        HttpSession session,
                        HttpServletResponse response) {

        User user = userRepository.findByNickname(nickname).orElse(null);

        // 사용자가 없거나 비밀번호가 틀린 경우
        if (user == null || !user.getPassword().equals(password)) {
            return "FAIL";
        }

        // 일반 세션 로그인 세팅
        session.setAttribute("loginUser", user);

        // 자동 로그인 체크박스를 활성화했다면 쿠키 발급 (30일 유지)
        if (rememberMe) {
            Cookie cookie = new Cookie("remember_me", user.getNickname());
            cookie.setMaxAge(60 * 60 * 24 * 30); // 30일을 초 단위로 환산
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        return "SUCCESS";
    }

    // 로그아웃 처리 (세션 무효화 및 자동 로그인 쿠키 파기)
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