package com.jsh.englishstudy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Expression {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long materialId; // 어떤 교재 소속인지
    private String category; // WORD, SENTENCE, DIALOGUE
    private String english;
    private String korean;
    private String speaker;
    private String dialogueTitle;
    private boolean isWrong; // 틀려서 오답노트에 들어갔는지 여부

    public Expression(Long materialId, String category, String english, String korean, String speaker) {
        this.materialId = materialId;
        this.category = category;
        this.english = english;
        this.korean = korean;
        this.speaker = speaker;
        this.dialogueTitle = dialogueTitle;
        this.isWrong = false; // 기본값은 정상 문장
    }
}