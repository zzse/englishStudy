package com.jsh.englishstudy.config;

import com.jsh.englishstudy.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect("/login");
            return false;
        }

        Optional<User> userOptional =
                Optional.ofNullable((User) session.getAttribute("loginUser"));

        boolean isValidUser = userOptional
                .filter(User::isRegistered) // ⭐ 여기서 정상 동작
                .isPresent();

        if (!isValidUser) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}