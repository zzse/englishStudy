package com.jsh.englishstudy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expression")
@Getter @Setter
@NoArgsConstructor
public class Expression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 영어 원문 (단어 / 문장 / 회화)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String english;

    // 한국어 뜻
    @Column(columnDefinition = "TEXT")
    private String korean;

    // WORD / SENTENCE / DIALOGUE
    @Column(nullable = false)
    private String category;

    // 화자 (회화용, 없으면 null)
    private String speaker;

    // 오답 여부
    @Column(name = "is_wrong", nullable = false)
    private Boolean wrong;

    @Column
    private String dialogueTitle;

    // 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    // 교재
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_material_id", nullable = false)
    @JsonBackReference
    private StudyMaterial studyMaterial;

    // 생성자 (회화)
    public Expression(StudyMaterial sm, User u,
                      String category,
                      String english,
                      String korean,
                      String speaker) {
        this.studyMaterial = sm;
        this.user = u;
        this.category = category;
        this.english = english;
        this.korean = korean;
        this.speaker = speaker;
        this.wrong = false;
    }

    // 생성자 (단어/문장)
    public Expression(StudyMaterial sm, User u,
                      String category,
                      String english,
                      String korean) {
        this.studyMaterial = sm;
        this.user = u;
        this.category = category;
        this.english = english;
        this.korean = korean;
        this.wrong = false;
    }
}