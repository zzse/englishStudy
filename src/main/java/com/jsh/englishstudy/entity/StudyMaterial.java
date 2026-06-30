package com.jsh.englishstudy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "study_material")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PDF 제목
    @Column(nullable = false)
    private String title;

    // 학습 날짜
    @Column(name = "study_date")
    private LocalDate studyDate;

    // 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public StudyMaterial(String title, LocalDate studyDate) {
        this.title = title;
        this.studyDate = studyDate;
    }
}