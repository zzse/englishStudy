package com.jsh.englishstudy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 모든 경로 감시하되
                .excludePathPatterns(
                        "/login", "/register",
                        "/api/auth/**",
                        "/css/**", "/js/**", "/images/**" // 로그인, 가입 관련 페이지 및 정적 파일은 예외 처리
                );
    }
}