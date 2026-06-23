package com.jsh.englishstudy.controller;

import com.jsh.englishstudy.entity.Expression;
import com.jsh.englishstudy.entity.StudyMaterial;
import com.jsh.englishstudy.repository.ExpressionRepository;
import com.jsh.englishstudy.repository.StudyMaterialRepository;
import com.jsh.englishstudy.service.PdfParserService; // 🚨 [필수] 서비스 임포트 추가
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException; // 🚨 [필수] IOException 임포트 추가
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final StudyMaterialRepository materialRepository;
    private final ExpressionRepository expressionRepository;
    private final PdfParserService pdfParserService; // 🚨 [여기 추가!] 스프링이 자동으로 주입해 주도록 필드를 선언합니다.

    // 테스트용 샘플 데이터 자동 삽입
    @PostConstruct
    public void initData() {
        StudyMaterial mat1 = materialRepository.save(new StudyMaterial("20260615 슈퍼 엘니뇨 뉴스", LocalDate.of(2026, 6, 15)));
        StudyMaterial mat2 = materialRepository.save(new StudyMaterial("20260622 교보문고 실전 회화", LocalDate.of(2026, 6, 22)));

        // 6월 15일 교재 데이터
        expressionRepository.save(new Expression(mat1.getId(), "WORD", "officially begun", "공식적으로 시작되다", null));
        expressionRepository.save(new Expression(mat1.getId(), "WORD", "intensify", "더 강해지다", null));
        expressionRepository.save(new Expression(mat1.getId(), "SENTENCE", "El Niño has officially begun, and it may intensify into a very strong event.", "엘니뇨가 공식적으로 시작됐고, 앞으로 매우 강한 현상으로 커질 가능성이 있습니다.", null));

        // 6월 22일 교재 데이터
        expressionRepository.save(new Expression(mat2.getId(), "WORD", "lightweight", "제형이 가벼운", null));
        expressionRepository.save(new Expression(mat2.getId(), "DIALOGUE", "Hi, are you looking for anything in particular?", "안녕하세요, 특별히 찾으시는 책 있으세요?", "A"));
        expressionRepository.save(new Expression(mat2.getId(), "DIALOGUE", "Yeah, I’m looking for something light.", "네, 가볍게 읽을 만한 책을 찾고 있어요.", "B"));

        // 미리 테스트용 오답 문장 하나 세팅 (전체 복습 리스트 확인용)
        Expression wrongSample = new Expression(mat1.getId(), "WORD", "knock-on effects", "연쇄적인 영향, 파급 효과", null);
        wrongSample.setWrong(true);
        expressionRepository.save(wrongSample);
    }

    // 1. 메인 화면 대시보드 (교재 목록 & 누적 오답 카운트)
    @GetMapping("/")
    public String indexPage(Model model) {
        model.addAttribute("materials", materialRepository.findAll());
        model.addAttribute("totalWrongCount", expressionRepository.findByIsWrong(true).size());
        return "index";
    }

    // 2. 날짜별 교재 진입 시 -> 단어/문장/회화 세부 선택 메뉴판
    @GetMapping("/review/material/{id}")
    public String materialMenu(@PathVariable Long id, Model model) {
        StudyMaterial mat = materialRepository.findById(id).orElseThrow();
        model.addAttribute("material", mat);
        return "material-menu";
    }

    // 3. 교재 메뉴판에서 최종 선택 시 -> 실제 한 장씩 푸는 퀴즈 화면 진입
    @GetMapping("/review/material/{id}/{category}")
    public String reviewMaterialCategory(@PathVariable Long id, @PathVariable String category, Model model) {
        StudyMaterial mat = materialRepository.findById(id).orElseThrow();
        List<Expression> list = expressionRepository.findByMaterialIdAndCategory(id, category.toUpperCase());

        if ("WORD".equalsIgnoreCase(category)) {
            Collections.shuffle(list); // 단어는 무작위 섞기
        }

        String typeTitle = "WORD".equalsIgnoreCase(category) ? "단어" : "SENTENCE".equalsIgnoreCase(category) ? "시사 문장" : "회화 롤플레잉";
        model.addAttribute("title", mat.getTitle() + " ➡️ " + typeTitle + " 테스트");
        model.addAttribute("expressions", list);
        return "review";
    }

    // 4. 누적 오답 창고 전체 복습 퀴즈 진입
    @GetMapping("/review/all-wrong")
    public String allWrongPage(Model model) {
        List<Expression> wrongList = expressionRepository.findByIsWrong(true);
        model.addAttribute("title", "🚨 누적 오답 창고 전체 복습");
        model.addAttribute("expressions", wrongList);
        return "review";
    }

    // [REST API] 사용자가 틀렸을 때 비동기로 호출되어 오답 마킹 수행
    @PostMapping("/api/expressions/{id}/wrong")
    @ResponseBody
    public String markAsWrong(@PathVariable Long id) {
        Expression exp = expressionRepository.findById(id).orElseThrow();
        exp.setWrong(true);
        expressionRepository.save(exp);
        return "success";
    }

    // 5. PDF 업로드 및 파싱 처리 API
    @PostMapping("/api/material/upload-pdf")
    public String uploadPdf(@RequestParam("pdfFile") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "redirect:/?error=empty";
            }

            // 1. 파일명 추출 (예: "20260615 슈퍼 엘니뇨 뉴스.pdf")
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) return "redirect:/?error=invalid_name";

            // 확장자 제거 ("20260615 슈퍼 엘니뇨 뉴스")
            String cleanName = originalFilename.substring(0, originalFilename.lastIndexOf(".")).trim();

            String title = cleanName;
            LocalDate studyDate = LocalDate.now(); // 파일명에 날짜가 없을 경우 오늘 날짜를 기본값으로 지정

            // 2. 정규식으로 앞자리 8자리 숫자(YYYYMMDD) 추출 시도
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})(.*)");
            java.util.regex.Matcher matcher = datePattern.matcher(cleanName);

            if (matcher.matches()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                // 자바 내장 타임 라이브러리로 안전하게 날짜 객체 생성 (오차 없음)
                studyDate = LocalDate.of(year, month, day);

                // 앞의 8자리 숫자를 제외한나머지 글자를 타이틀로 지정
                title = matcher.group(4).trim();
                if (title.isEmpty()) {
                    title = "스터디 교재 (" + studyDate + ")";
                }
            }

            // 3. 새 교재 마스터 정보 저장
            StudyMaterial newMaterial = new StudyMaterial(title, studyDate);
            StudyMaterial savedMaterial = materialRepository.save(newMaterial);

            // 4. PDF 파싱 서비스를 통해 단어/문장/회화 리스트 추출 및 저장
            List<Expression> parsedExpressions = pdfParserService.parsePdfToExpressions(file, savedMaterial.getId());
            expressionRepository.saveAll(parsedExpressions);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/?error=upload_failed";
        }

        return "redirect:/"; // 성공 시 메인 대시보드로 새로고침
    }

    @PostMapping("/api/expressions/{id}/clear-wrong")
    @ResponseBody
    public String clearFromWrong(@PathVariable Long id) {
        Expression exp = expressionRepository.findById(id).orElseThrow();
        exp.setWrong(false); // 🚨 false로 되돌려서 오답 창고에서 제외시킵니다.
        expressionRepository.save(exp);
        return "success";
    }

}