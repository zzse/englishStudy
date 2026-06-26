package com.jsh.englishstudy.config;

import com.jsh.englishstudy.entity.User;
import com.jsh.englishstudy.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 🚨 [인프라 꼬임을 원천 차단하는 무적의 프리패스 라인]
        // 어떤 경로로 우회해서 들어오든, 현재 요청에 /login, /register, 정적 파일(css/js)이 묻어있다면
        // 세션이고 쿠키고 나발이고 무조건 통과시켜야 302 리디렉션 늪에 안 빠집니다!
        if (requestURI.equals("/login") || requestURI.equals("/register")
                || requestURI.contains("/css") || requestURI.contains("/js") || requestURI.contains("/images")
                || requestURI.equals("/error")) {
            return true;
        }

        // 1. 이미 세션 로그인이 되어 있으면 패스
        if (loginUser != null) {
            return true;
        }

        // 2. 자동 로그인 쿠키 검사
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember_me".equals(cookie.getName())) {
                    String nickname = cookie.getValue();
                    User user = userRepository.findByNickname(nickname).orElse(null);

                    if (user != null && user.isRegistered()) {
                        session.setAttribute("loginUser", user);
                        return true;
                    }
                }
            }
        }

        // 3. 둘 다 없으면 로그인 페이지로 강제 이동
        response.sendRedirect("/login");
        return false;
    }
}