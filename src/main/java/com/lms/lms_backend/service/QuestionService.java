package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Exam;
import com.lms.lms_backend.entity.Question;
import com.lms.lms_backend.repository.ExamRepository;
import com.lms.lms_backend.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;

    public QuestionService(QuestionRepository questionRepository, ExamRepository examRepository) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
    }

    public List<Question> getQuestionsByExam(Long examId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam != null) {
            return questionRepository.findByExamOrderByOrderNumberAsc(exam);
        }
        return List.of();
    }

    public Question getQuestionById(Long id) {
        return questionRepository.findById(id).orElse(null);
    }

    @Transactional
    public Question addQuestion(Long examId, Question question) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));

        question.setExam(exam);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());

        // Set order number
        long questionCount = questionRepository.countByExam(exam);
        question.setOrderNumber((int) questionCount + 1);

        Question saved = questionRepository.save(question);

        // Update exam total marks and questions count
        updateExamStats(exam);

        return saved;
    }

    @Transactional
    public List<Question> addMultipleQuestions(Long examId, List<Question> questions) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));

        long currentCount = questionRepository.countByExam(exam);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            q.setExam(exam);
            q.setOrderNumber((int) currentCount + i + 1);
            q.setCreatedAt(LocalDateTime.now());
            q.setUpdatedAt(LocalDateTime.now());
        }

        List<Question> saved = questionRepository.saveAll(questions);
        updateExamStats(exam);

        return saved;
    }

    @Transactional
    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = getQuestionById(id);
        if (question == null) {
            throw new RuntimeException("Question not found!");
        }

        question.setQuestionText(questionDetails.getQuestionText());
        question.setQuestionType(questionDetails.getQuestionType());
        question.setOptionA(questionDetails.getOptionA());
        question.setOptionB(questionDetails.getOptionB());
        question.setOptionC(questionDetails.getOptionC());
        question.setOptionD(questionDetails.getOptionD());
        question.setCorrectAnswer(questionDetails.getCorrectAnswer());
        question.setMarks(questionDetails.getMarks());
        question.setExplanation(questionDetails.getExplanation());
        question.setUpdatedAt(LocalDateTime.now());

        Question updated = questionRepository.save(question);
        updateExamStats(question.getExam());

        return updated;
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        if (question != null) {
            Exam exam = question.getExam();
            questionRepository.delete(question);
            updateExamStats(exam);
            // Reorder remaining questions
            reorderQuestions(exam.getId());
        }
    }

    private void updateExamStats(Exam exam) {
        long totalQuestions = questionRepository.countByExam(exam);
        List<Question> questions = questionRepository.findByExamOrderByOrderNumberAsc(exam);
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();

        exam.setTotalQuestions((int) totalQuestions);
        exam.setTotalMarks(totalMarks);
        exam.setUpdatedAt(LocalDateTime.now());
        examRepository.save(exam);
    }

    private void reorderQuestions(Long examId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam != null) {
            List<Question> questions = questionRepository.findByExamOrderByOrderNumberAsc(exam);
            for (int i = 0; i < questions.size(); i++) {
                questions.get(i).setOrderNumber(i + 1);
            }
            questionRepository.saveAll(questions);
        }
    }
}