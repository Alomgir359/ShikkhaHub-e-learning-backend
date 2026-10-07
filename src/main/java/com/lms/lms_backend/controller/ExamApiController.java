package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.ExamAttempt;
import com.lms.lms_backend.entity.Question;
import com.lms.lms_backend.service.ExamAttemptService;
import com.lms.lms_backend.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class ExamApiController {

    private final ExamAttemptService examAttemptService;
    private final QuestionService questionService;

    public ExamApiController(ExamAttemptService examAttemptService, QuestionService questionService) {
        this.examAttemptService = examAttemptService;
        this.questionService = questionService;
    }

    @PostMapping("/save-answer")
    public ResponseEntity<Map<String, Object>> saveAnswer(@RequestBody Map<String, Object> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long attemptId = Long.valueOf(request.get("attemptId").toString());
            Long questionId = Long.valueOf(request.get("questionId").toString());
            String answer = (String) request.get("answer");

            examAttemptService.saveAnswer(attemptId, questionId, answer);
            response.put("success", true);
            response.put("message", "Answer saved!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/tab-switch/{attemptId}")
    public ResponseEntity<Map<String, Object>> recordTabSwitch(@PathVariable Long attemptId) {
        Map<String, Object> response = new HashMap<>();
        try {
            examAttemptService.recordTabSwitch(attemptId);
            response.put("success", true);
            response.put("message", "Tab switch recorded");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/time-left/{attemptId}")
    public ResponseEntity<Map<String, Object>> getTimeLeft(@PathVariable Long attemptId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ExamAttempt attempt = examAttemptService.getAttemptById(attemptId);
            if (attempt != null && attempt.getStartTime() != null) {
                long elapsedSeconds = (System.currentTimeMillis() - attempt.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC)) * 1000;
                long examDurationSeconds = attempt.getExam().getDurationMinutes() * 60L;
                long remainingSeconds = Math.max(0, examDurationSeconds - elapsedSeconds);
                response.put("success", true);
                response.put("remainingSeconds", remainingSeconds);
            } else {
                response.put("success", false);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/auto-submit/{attemptId}")
    public ResponseEntity<Map<String, Object>> autoSubmit(@PathVariable Long attemptId) {
        Map<String, Object> response = new HashMap<>();
        try {
            examAttemptService.autoSubmitExam(attemptId);
            response.put("success", true);
            response.put("message", "Exam auto-submitted!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}