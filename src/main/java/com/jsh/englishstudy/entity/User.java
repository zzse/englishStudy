package com.jsh.englishstudy.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "users")
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String password;

    /**
     * 회원가입 완료 여부
     * DB: is_registered NOT NULL
     */
    @Column(name = "is_registered", nullable = false)
    private boolean registered = true; // ⭐ 기본값 필수

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        this.registered = true; // ⭐ 명시적 초기화
    }

    // ===== Getter =====
    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPassword() {
        return password;
    }

    // ⭐ LoginInterceptor에서 사용
    public boolean isRegistered() {
        return registered;
    }

    // ===== Setter =====
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}