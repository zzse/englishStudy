package com.jsh.englishstudy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
public class StudyMaterial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // 예: 260615 슈퍼 엘니뇨
    private LocalDate studyDate; // 스터디 날짜 (날짜별 조회용)

    public StudyMaterial(String title, LocalDate studyDate) {
        this.title = title;
        this.studyDate = studyDate;
    }
}