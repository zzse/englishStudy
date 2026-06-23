package com.jsh.englishstudy.controller;

import com.jsh.englishstudy.entity.Expression;
import com.jsh.englishstudy.entity.StudyMaterial;
import com.jsh.englishstudy.repository.ExpressionRepository;
import com.jsh.englishstudy.repository.StudyMaterialRepository;
import com.jsh.englishstudy.service.PdfParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final StudyMaterialRepository materialRepository;
    private final ExpressionRepository expressionRepository;
    private final PdfParserService pdfParserService;

    // 🚨 [수정 완료] 자꾸 중복 데이터를 만들던 @PostConstruct (initData) 구역을 완전히 삭제했습니다.
    // 이제 서버를 아무리 껐다 켜도 DB 파일이 마음대로 더러워지지 않습니다.

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

    // 5. PDF 업로드 및 파싱 처리 API (★ 오직 이 주소를 통해서만 데이터가 새롭게 인서트됩니다 ★)
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
            LocalDate studyDate = LocalDate.now();

            // 2. 정규식으로 앞자리 8자리 숫자(YYYYMMDD) 추출 시도
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})(.*)");
            java.util.regex.Matcher matcher = datePattern.matcher(cleanName);

            if (matcher.matches()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                studyDate = LocalDate.of(year, month, day);

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
        exp.setWrong(false);
        expressionRepository.save(exp);
        return "success";
    }

}