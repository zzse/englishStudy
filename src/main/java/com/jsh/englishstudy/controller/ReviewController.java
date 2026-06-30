package com.jsh.englishstudy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsh.englishstudy.entity.Expression;
import com.jsh.englishstudy.entity.StudyMaterial;
import com.jsh.englishstudy.entity.User;
import com.jsh.englishstudy.repository.ExpressionRepository;
import com.jsh.englishstudy.repository.StudyMaterialRepository;
import com.jsh.englishstudy.service.PdfParserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final StudyMaterialRepository materialRepository;
    private final ExpressionRepository expressionRepository;
    private final PdfParserService pdfParserService;
    private final ObjectMapper objectMapper;

    /**
     * 메인 대시보드
     */
    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        List<StudyMaterial> materials = materialRepository.findByUser(loginUser);
        model.addAttribute("materials", materials);

        List<Expression> wrongList = expressionRepository.findByUserAndWrong(loginUser, true);
        model.addAttribute("totalWrongCount", wrongList.size());

        return "index";
    }

    @GetMapping("/material/{id}/menu")
    public String materialMenu(@PathVariable Long id,
                               HttpSession session,
                               Model model) {

        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return "redirect:/login";
        }

        StudyMaterial material =
                materialRepository.findById(id).orElse(null);

        if (material == null
                || !material.getUser().getId().equals(user.getId())) {
            return "redirect:/";
        }

        model.addAttribute("material", material);
        return "material-menu";
    }


    /**
     * PDF 업로드 & 파싱
     */
    @PostMapping("/api/material/upload-pdf")
    @ResponseBody
    public ResponseEntity<?> uploadPdf(
            @RequestParam("pdfFile") MultipartFile pdfFile,
            HttpSession session) {

        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (pdfFile == null || pdfFile.isEmpty()
                || !pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Invalid PDF file");
        }

        try {
            // 1️⃣ 교재 저장
            StudyMaterial material = new StudyMaterial(
                    pdfFile.getOriginalFilename(),
                    LocalDate.now()
            );
            material.setUser(user);
            materialRepository.save(material);

            List<Expression> expressions =
                    pdfParserService.parsePdfToExpressions(pdfFile, material, user);

            expressions.forEach(e -> e.setWrong(false));

            expressionRepository.saveAll(expressions);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 학습 세트 삭제
     */
    @PostMapping("/api/material/{id}/delete")
    @ResponseBody
    public String deleteMaterial(@PathVariable Long id,
                                 HttpSession session) {

        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return "unauthorized";
        }

        StudyMaterial material =
                materialRepository.findById(id).orElse(null);

        if (material == null
                || !material.getUser().getId().equals(user.getId())) {
            return "unauthorized";
        }

        // 🔥 1️⃣ Expression 먼저 삭제
        expressionRepository.deleteByStudyMaterialId(id);

        // 🔥 2️⃣ StudyMaterial 삭제
        materialRepository.delete(material);

        return "success";
    }

    /**
     * 교재 + 카테고리별 복습 화면
     */
    @GetMapping("/review/material/{id}/{category}")
    public String reviewMaterial(
            @PathVariable Long id,
            @PathVariable String category,
            HttpSession session,
            Model model) throws Exception {

        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return "redirect:/login";
        }

        String normalizedCategory = category.toUpperCase();

        List<Expression> expressions =
                expressionRepository.findByStudyMaterialIdAndCategory(
                        id,
                        normalizedCategory
                );

        model.addAttribute(
                "expressionsJson",
                objectMapper.writeValueAsString(expressions)
        );

        return "review";
    }

    @PostMapping("/api/expressions/{id}/wrong")
    @ResponseBody
    public String markAsWrong(@PathVariable Long id) {
        expressionRepository.findById(id).ifPresent(e -> {
            e.setWrong(true);
            expressionRepository.save(e);
        });
        return "success";
    }

    @PostMapping("/api/expressions/{id}/clear-wrong")
    @ResponseBody
    public String clearWrong(@PathVariable Long id) {
        expressionRepository.findById(id).ifPresent(e -> {
            e.setWrong(false);
            expressionRepository.save(e);
        });
        return "success";
    }

    @GetMapping("/review/all-wrong")
    public String reviewAllWrong(HttpSession session, Model model) throws Exception {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        // 1. 해당 유저의 오답(wrong=true)만 데이터베이스에서 모두 조회
        List<Expression> list = expressionRepository.findByUserAndWrong(loginUser, true);

        // 2. 화면에 던져주기 위해 JSON으로 변환
        model.addAttribute("expressionsJson", objectMapper.writeValueAsString(list));

        // 3. 화면 상단 타이틀 설정
        model.addAttribute("title", "🔥 전체 오답 마스터");

        // 4. review.html을 그대로 재사용! (HTML을 또 만들 필요가 없습니다)
        return "review";
    }

}

