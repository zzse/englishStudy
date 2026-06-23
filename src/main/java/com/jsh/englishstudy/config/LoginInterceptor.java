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
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 1. 이미 세션 로그인이 되어 있으면 패스
        if (loginUser != null) {
            return true;
        }

        // 2. 세션은 없는데 브라우저에 '자동 로그인 쿠키'가 남아있는지 검사
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember_me".equals(cookie.getName())) {
                    String nickname = cookie.getValue();
                    User user = userRepository.findByNickname(nickname).orElse(null);

                    if (user != null && user.isRegistered()) {
                        // 쿠키 유효성 통과 시 자동으로 세션 생성 (자동로그인 성공!)
                        session.setAttribute("loginUser", user);
                        return true;
                    }
                }
            }
        }

        // 3. 둘 다 없으면 로그인 페이지로 튕겨내기
        response.sendRedirect("/login");
        return false;
    }
}