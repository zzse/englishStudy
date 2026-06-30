package com.jsh.englishstudy.service;

import com.jsh.englishstudy.entity.Expression;
import com.jsh.englishstudy.entity.StudyMaterial;
import com.jsh.englishstudy.entity.User;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class PdfParserService {

    private enum Section {
        NONE, WORD, SENTENCE, CONVERSATION
    }

    public List<Expression> parsePdfToExpressions(
            MultipartFile file,
            StudyMaterial studyMaterial,
            User user
    ) throws Exception {

        List<Expression> expressions = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is))) {

            PDFTextStripper stripper = new PDFTextStripper();
            String[] lines = stripper.getText(document).split("\\r?\\n");

            Section section = Section.NONE;

            StringBuilder engBuf = new StringBuilder();
            StringBuilder korBuf = new StringBuilder();

            Pattern dialoguePattern = Pattern.compile("^([AB])\\s*[:.]\\s*(.+)$");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String upper = line.replaceAll("\\s+", "").toUpperCase();

                /* =====================
                   TALK ABOUT IT 이후 전부 차단
                ====================== */
                if (upper.contains("TALKABOUTIT")) {
                    section = Section.NONE;
                    continue;
                }

                /* =====================
                   🚀 [수정됨] 섹션 전환을 Garbage 검사보다 먼저 실행!
                   (K E Y P H R A S E S가 Garbage로 날아가는 것 방지)
                ====================== */
                if (upper.contains("KEYPHRASES")) {
                    flushSentence(expressions, engBuf, korBuf, studyMaterial, user);
                    section = Section.WORD;
                    continue;
                }

                if (upper.contains("NEWSSUMMARY") || upper.contains("PARAGRAPH")) {
                    flushSentence(expressions, engBuf, korBuf, studyMaterial, user);
                    section = Section.SENTENCE;
                    continue;
                }

                if (upper.contains("CONVERSATION")) {
                    flushSentence(expressions, engBuf, korBuf, studyMaterial, user);
                    section = Section.CONVERSATION;
                    continue;
                }

                /* =====================
                   Garbage 제거
                ====================== */
                if (isGarbageLine(line)) continue;


                /* =====================
                   WORD (KEY PHRASES)
                ====================== */
                if (section == Section.WORD) {

                    // 소제목 제거
                    if (line.matches("^\\d+\\..*")) continue;

                    String english = line
                            .replaceFirst("^\\d+\\s*[.)-]?\\s*", "")
                            .replaceFirst("^[•●■▪▶\\-]+\\s*", "")
                            .replace("★", "")
                            .trim();

                    // 🚀 [추가됨] 추출된 영어 문장에 한글이 섞여있다면, 영단어가 아니라 '설명글'이므로 무시합니다.
                    if (containsKorean(english)) continue;
                    if (!english.matches(".*[a-zA-Z].*")) continue;
                    if (english.length() > 100) continue; // 비정상적으로 긴 문장 방어

                    String korean = "";
                    if (i + 1 < lines.length && containsKorean(lines[i + 1])) {
                        korean = lines[++i].trim();
                    }

                    expressions.add(new Expression(
                            studyMaterial,
                            user,
                            "WORD",
                            english,
                            korean
                    ));
                    continue;
                }

                /* =====================
                   SENTENCE
                ====================== */
                if (section == Section.SENTENCE) {

                    if (line.matches("^\\d+\\..*")) continue;

                    if (isEnglish(line)) {
                        flushSentence(expressions, engBuf, korBuf, studyMaterial, user);
                        engBuf.append(line);
                    } else if (containsKorean(line)) {
                        korBuf.append(line);
                    }
                    continue;
                }

                /* =====================
                   CONVERSATION
                ====================== */
                if (section == Section.CONVERSATION) {

                    if (line.matches("^\\d+\\..*")) continue; // 소제목 방어

                    Matcher m = dialoguePattern.matcher(line);
                    if (!m.matches()) {
                        if (containsKorean(line)) {
                            // 현재 화자를 찾을 수 없을 때 한글 해석이 나오면 최근 대화문에 붙임
                            for (int k = expressions.size() - 1; k >= 0; k--) {
                                if ("DIALOGUE".equals(expressions.get(k).getCategory()) && expressions.get(k).getKorean().isEmpty()) {
                                    expressions.get(k).setKorean(line);
                                    break;
                                }
                            }
                        }
                        continue;
                    }

                    String speaker = m.group(1);
                    String content = m.group(2).trim();

                    if (isEnglish(content)) {
                        expressions.add(new Expression(
                                studyMaterial,
                                user,
                                "DIALOGUE",
                                content,
                                "",
                                speaker
                        ));
                    } else {
                        Expression last = lastDialogue(expressions, speaker);
                        if (last != null) last.setKorean(content);
                    }
                }
            }

            flushSentence(expressions, engBuf, korBuf, studyMaterial, user);
        }

        System.out.println("🔥 파싱 결과 수 = " + expressions.size());
        return expressions;
    }

    /* =====================
       유틸
    ====================== */

    private void flushSentence(List<Expression> list,
                               StringBuilder eng,
                               StringBuilder kor,
                               StudyMaterial sm,
                               User user) {

        if (eng.length() > 0 && kor.length() > 0) {
            list.add(new Expression(
                    sm,
                    user,
                    "SENTENCE",
                    eng.toString().trim(),
                    kor.toString().trim()
            ));
            eng.setLength(0);
            kor.setLength(0);
        }
    }

    private Expression lastDialogue(List<Expression> list, String speaker) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Expression e = list.get(i);
            if ("DIALOGUE".equals(e.getCategory())
                    && speaker.equals(e.getSpeaker())
                    && e.getKorean().isEmpty()) {
                return e;
            }
        }
        return null;
    }

    private boolean containsKorean(String s) {
        return s.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*");
    }

    private boolean isEnglish(String s) {
        return s.matches(".*[a-zA-Z].*") && !containsKorean(s);
    }

    private boolean isGarbageLine(String line) {
        String u = line.toUpperCase();

        if (u.contains("JUDE ENG")) return true;
        if (u.contains("UPGRADE YOUR")) return true;
        if (u.contains("NEWS PAGE")) return true;

        // 알파벳 띄어쓰기 제목 (섹션 체크 밑으로 내렸으므로 이제 안전합니다V)
        if (line.matches("([A-Z]\\s+){3,}[A-Z]")) return true;

        return false;
    }
}