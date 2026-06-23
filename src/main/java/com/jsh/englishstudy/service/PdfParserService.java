package com.jsh.englishstudy.service;

import com.jsh.englishstudy.entity.Expression;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParserService {

    public List<Expression> parsePdfToExpressions(MultipartFile file, Long materialId) throws IOException {
        List<Expression> expressions = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            String[] lines = text.split("\\r?\\n");
            String currentSection = "NONE";

            // 기본 대화방 상황 타이틀
            String currentDialogueTitle = "일반 회화";

            StringBuilder engBuffer = new StringBuilder();
            StringBuilder korBuffer = new StringBuilder();

            // 🚨 대화문(A: 또는 B:)을 잡아내는 완벽한 정규식 패턴 (공백/특수문자 포함)
            Pattern dialoguePattern = Pattern.compile(".*([A-B])\\s*:\\s*(.*)");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                // 시스템 노이즈 텍스트 필터링
                if (line.contains("Page") || line.contains("UPGRADE") || line.contains("COMPREHENSION") ||
                        line.contains("핵심 표현") || line.contains("관련 표현") || line.contains("도서 관련") || line.contains("뷰티")) {
                    // 🚨 단, 대화 섹션의 핵심 단어인 DIALOGUE 제목 자체는 패스하되 상태 변경용으로 체크 안 함
                    continue;
                }

                // 🚨 [치트키 옵션 1] 현재 섹션 위치와 상관없이, 줄 어디선가 "A:" 나 "B:"가 감지되면 무조건 대화문으로 우선 처리!!
                Matcher dialogueMatcher = dialoguePattern.matcher(line);
                if (dialogueMatcher.matches()) {
                    String speaker = dialogueMatcher.group(1).trim(); // A 또는 B
                    String content = dialogueMatcher.group(2).trim(); // 대사 내용

                    if (!containsKorean(content)) {
                        // 영어 대사 저장
                        Expression dialogueExp = new Expression(materialId, "DIALOGUE", content, "[회화] 번역문을 입력하세요.", speaker);
                        dialogueExp.setDialogueTitle(currentDialogueTitle);
                        expressions.add(dialogueExp);
                    } else {
                        // 한글 번역 대사 매칭 (직전 대사에 번역 주입)
                        if (!expressions.isEmpty()) {
                            Expression lastExp = expressions.get(expressions.size() - 1);
                            if ("DIALOGUE".equals(lastExp.getCategory()) && speaker.equals(lastExp.getSpeaker())) {
                                lastExp.setKorean(content);
                            }
                        }
                    }
                    continue; // 대화문 처리 완료 시 아래 일반 섹션 로직은 건너뜀!
                }

                // 🚨 [치트키 옵션 2] 줄 시작이 "6. 교보문고에 가다" 처럼 숫자로 시작하고 한글이 포함되어 있으면 대화방 제목으로 갱신!!
                if (line.matches("^\\d+\\.\\s*.*") && containsKorean(line)) {
                    currentDialogueTitle = line;
                    continue;
                }

                // 공백 제거 후 상단 섹션 전환 감지 (WORD와 SENTENCE 섹션용)
                String collapsedLine = line.replaceAll("\\s+", "").toUpperCase();

                if (collapsedLine.contains("KEYPHRASES")) {
                    currentSection = "WORD_SECTION";
                    continue;
                } else if (collapsedLine.contains("NEWSSUMMARY")) {
                    currentSection = "SENTENCE_SECTION";
                    continue;
                } else if (collapsedLine.contains("TALKABOUTIT")) {
                    if (engBuffer.length() > 0) {
                        expressions.add(new Expression(materialId, "SENTENCE", engBuffer.toString().trim(), korBuffer.toString().trim(), null));
                        engBuffer.setLength(0);
                        korBuffer.setLength(0);
                    }
                    currentSection = "NONE";
                    continue;
                }

                // 일반 섹션 파싱 로직 (WORD, SENTENCE)
                switch (currentSection) {
                    case "WORD_SECTION":
                        if (line.matches("^\\d+\\s+.*")) {
                            String english = line.replaceAll("^\\d+\\s+", "").trim();
                            String korean = "[단어] 뜻을 입력하세요.";

                            if (i + 1 < lines.length && !lines[i + 1].trim().isEmpty() && !lines[i + 1].trim().matches("^\\d+\\s+.*")) {
                                korean = lines[i + 1].trim();
                                i++;
                            }
                            expressions.add(new Expression(materialId, "WORD", english, korean, null));
                        }
                        break;

                    case "SENTENCE_SECTION":
                        if (line.matches("^\\d+\\.\\s*English.*")) {
                            continue;
                        }

                        if (!containsKorean(line)) {
                            if (korBuffer.length() > 0) {
                                expressions.add(new Expression(materialId, "SENTENCE", engBuffer.toString().trim(), korBuffer.toString().trim(), null));
                                engBuffer.setLength(0);
                                korBuffer.setLength(0);
                            }
                            if (engBuffer.length() > 0) engBuffer.append(" ");
                            engBuffer.append(line);
                        } else {
                            if (korBuffer.length() > 0) korBuffer.append(" ");
                            korBuffer.append(line);
                        }
                        break;

                    default:
                        break;
                }
            }

            if (engBuffer.length() > 0) {
                expressions.add(new Expression(materialId, "SENTENCE", engBuffer.toString().trim(), korBuffer.toString().trim(), null));
            }
        }

        return expressions;
    }

    private boolean containsKorean(String text) {
        return text.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*");
    }
}