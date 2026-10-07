package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.*;
import com.lms.lms_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExamAttemptService {

    private final ExamAttemptRepository examAttemptRepository;
    private final AnswerRepository answerRepository;
    private final ExamRepository examRepository;
    private final EmployeeRepository employeeRepository;
    private final QuestionRepository questionRepository;

    public ExamAttemptService(ExamAttemptRepository examAttemptRepository,
                              AnswerRepository answerRepository,
                              ExamRepository examRepository,
                              EmployeeRepository employeeRepository,
                              QuestionRepository questionRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.answerRepository = answerRepository;
        this.examRepository = examRepository;
        this.employeeRepository = employeeRepository;
        this.questionRepository = questionRepository;
    }

    public ExamAttempt getAttemptById(Long attemptId) {
        return examAttemptRepository.findById(attemptId).orElse(null);
    }

    public List<Answer> getAnswersForAttempt(Long attemptId) {
        ExamAttempt attempt = getAttemptById(attemptId);
        if (attempt != null) {
            return answerRepository.findByExamAttempt(attempt);
        }
        return List.of();
    }

    @Transactional
    public ExamAttempt startExam(Long examId, Long employeeId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found!"));

        // Check if already attempted
        if (examAttemptRepository.existsByExamAndEmployee(exam, employee)) {
            Optional<ExamAttempt> existing = examAttemptRepository.findByExamAndEmployeeAndStatus(exam, employee, "IN_PROGRESS");
            if (existing.isPresent()) {
                return existing.get();
            }
            throw new RuntimeException("You have already attempted this exam!");
        }

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExam(exam);
        attempt.setEmployee(employee);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setStatus("IN_PROGRESS");
        attempt.setTabSwitchCount(0);
        attempt.setIsPenaltyApplied(false);

        return examAttemptRepository.save(attempt);
    }

    @Transactional
    public void saveAnswer(Long attemptId, Long questionId, String answer) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found!"));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new RuntimeException("Exam already completed!");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found!"));

        Optional<Answer> existingAnswer = answerRepository.findByExamAttemptAndQuestion(attempt, question);

        Answer answerEntity;
        if (existingAnswer.isPresent()) {
            answerEntity = existingAnswer.get();
        } else {
            answerEntity = new Answer();
            answerEntity.setExamAttempt(attempt);
            answerEntity.setQuestion(question);
        }

        answerEntity.setGivenAnswer(answer);
        answerEntity.setAnsweredAt(LocalDateTime.now());

        // Evaluate answer for MCQ
        if ("MCQ".equals(question.getQuestionType())) {
            boolean isCorrect = question.getCorrectAnswer() != null &&
                    question.getCorrectAnswer().equalsIgnoreCase(answer);
            answerEntity.setIsCorrect(isCorrect);
            answerEntity.setObtainedMarks(isCorrect ? question.getMarks() : 0);
        } else {
            // For short and broad questions, marks need to be assigned manually by admin
            answerEntity.setIsCorrect(null);
            answerEntity.setObtainedMarks(0);
        }

        answerRepository.save(answerEntity);

        // Update total obtained marks
        updateObtainedMarks(attempt);
    }

    private void updateObtainedMarks(ExamAttempt attempt) {
        List<Answer> answers = answerRepository.findByExamAttempt(attempt);
        int totalMarks = answers.stream()
                .filter(a -> a.getObtainedMarks() != null)
                .mapToInt(Answer::getObtainedMarks)
                .sum();
        attempt.setObtainedMarks(totalMarks);
        examAttemptRepository.save(attempt);
    }

    @Transactional
    public ExamAttempt submitExam(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found!"));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new RuntimeException("Exam already submitted!");
        }

        attempt.setEndTime(LocalDateTime.now());
        attempt.setStatus("COMPLETED");

        return examAttemptRepository.save(attempt);
    }

    @Transactional
    public ExamAttempt autoSubmitExam(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found!"));

        if ("IN_PROGRESS".equals(attempt.getStatus())) {
            attempt.setEndTime(LocalDateTime.now());
            attempt.setStatus("AUTO_SUBMITTED");
            return examAttemptRepository.save(attempt);
        }

        return attempt;
    }

    @Transactional
    public void recordTabSwitch(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found!"));

        int newCount = attempt.getTabSwitchCount() + 1;
        attempt.setTabSwitchCount(newCount);

        // Apply penalty after 3 tab switches
        if (newCount >= 3 && !attempt.getIsPenaltyApplied()) {
            attempt.setIsPenaltyApplied(true);
            // Deduct 10% marks as penalty
            int penaltyMarks = (int) Math.round(attempt.getObtainedMarks() * 0.1);
            attempt.setObtainedMarks(Math.max(0, attempt.getObtainedMarks() - penaltyMarks));
        }

        examAttemptRepository.save(attempt);
    }

    public ExamAttempt getActiveAttempt(Long examId, Long employeeId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        if (exam != null && employee != null) {
            return examAttemptRepository.findByExamAndEmployeeAndStatus(exam, employee, "IN_PROGRESS").orElse(null);
        }
        return null;
    }

    public boolean hasAttempted(Long examId, Long employeeId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        if (exam != null && employee != null) {
            return examAttemptRepository.existsByExamAndEmployee(exam, employee);
        }
        return false;
    }

    public List<ExamAttempt> getEmployeeAttempts(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee != null) {
            return examAttemptRepository.findByEmployee(employee);
        }
        return List.of();
    }

    public Map<String, Object> getExamResults(Long examId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return Map.of();

        List<ExamAttempt> attempts = examAttemptRepository.findCompletedAttemptsByExam(exam);
        Double averageMarks = examAttemptRepository.getAverageMarksForExam(exam);

        Map<String, Object> results = new HashMap<>();
        results.put("examName", exam.getExamName());
        results.put("totalParticipants", attempts.size());
        results.put("averageMarks", averageMarks != null ? averageMarks : 0);
        results.put("totalMarks", exam.getTotalMarks());
        results.put("passingMarks", exam.getPassingMarks());

        List<Map<String, Object>> participantDetails = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("employeeName", attempt.getEmployee().getFullName());
            detail.put("employeeId", attempt.getEmployee().getEmployeeId());
            detail.put("rank", attempt.getEmployee().getRank() != null ? attempt.getEmployee().getRank().getRankName() : "N/A");
            detail.put("obtainedMarks", attempt.getObtainedMarks());
            detail.put("percentage", (attempt.getObtainedMarks() * 100.0) / exam.getTotalMarks());
            detail.put("tabSwitches", attempt.getTabSwitchCount());
            detail.put("status", attempt.getObtainedMarks() >= exam.getPassingMarks() ? "PASSED" : "FAILED");
            participantDetails.add(detail);
        }

        results.put("participants", participantDetails);
        return results;
    }

    @Transactional
    public void evaluateDescriptiveQuestion(Long answerId, Integer marks) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found!"));

        answer.setObtainedMarks(marks);
        answer.setIsCorrect(marks > 0);
        answerRepository.save(answer);

        updateObtainedMarks(answer.getExamAttempt());
    }
}