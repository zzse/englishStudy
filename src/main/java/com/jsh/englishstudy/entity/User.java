package com.jsh.englishstudy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nickname; // 스터디원 닉네임

    private String password; // 암호화 또는 평문 비밀번호 (스터디용이므로 단순 스트링 처리)

    @Column(nullable = false)
    private boolean isRegistered = false; // 회원가입(비번 설정) 완료 여부

    public User(String nickname) {
        this.nickname = nickname;
        this.isRegistered = false;
    }
}