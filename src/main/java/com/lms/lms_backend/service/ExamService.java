package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Employee;
import com.lms.lms_backend.entity.Exam;
import com.lms.lms_backend.entity.Question;
import com.lms.lms_backend.entity.Rank;
import com.lms.lms_backend.repository.EmployeeRepository;
import com.lms.lms_backend.repository.ExamRepository;
import com.lms.lms_backend.repository.QuestionRepository;
import com.lms.lms_backend.repository.RankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final RankRepository rankRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    private QuestionRepository questionRepository;

    public ExamService(ExamRepository examRepository, RankRepository rankRepository, EmployeeRepository employeeRepository) {
        this.examRepository = examRepository;
        this.rankRepository = rankRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public List<Exam> getPublishedExams() {
        return examRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
    }

    public List<Exam> getDraftExams() {
        return examRepository.findByStatus("DRAFT");
    }

    public Exam getExamById(Long id) {
        return examRepository.findById(id).orElse(null);
    }

    public List<Exam> getActiveExams() {
        return examRepository.findActiveExams(LocalDateTime.now());
    }

    public List<Exam> getExamsForEmployee(Employee employee) {
        if (employee.getRank() != null) {
            return examRepository.findExamsByRankPriority(
                    employee.getRank().getPriority(),
                    employee.getRank().getPriority()
            );
        }
        return List.of();
    }

    public List<Question> getQuestionsForExam(Long examId) {
        Exam exam = getExamById(examId);
        if (exam != null) {
            return questionRepository.findByExamOrderByOrderNumberAsc(exam);
        }
        return List.of();
    }

    @Transactional
    public Exam createExam(Exam exam, Long fromRankId, Long toRankId) {
        Rank fromRank = rankRepository.findById(fromRankId)
                .orElseThrow(() -> new RuntimeException("From Rank not found!"));
        Rank toRank = rankRepository.findById(toRankId)
                .orElseThrow(() -> new RuntimeException("To Rank not found!"));

        exam.setFromRank(fromRank);
        exam.setToRank(toRank);
        exam.setStatus("DRAFT");
        exam.setCreatedAt(LocalDateTime.now());
        exam.setUpdatedAt(LocalDateTime.now());

        return examRepository.save(exam);
    }

    @Transactional
    public Exam updateExam(Long id, Exam examDetails, Long fromRankId, Long toRankId) {
        Exam exam = getExamById(id);
        if (exam == null) {
            throw new RuntimeException("Exam not found!");
        }

        exam.setExamName(examDetails.getExamName());
        exam.setDescription(examDetails.getDescription());
        exam.setDurationMinutes(examDetails.getDurationMinutes());
        exam.setExamDate(examDetails.getExamDate());
        exam.setStartTime(examDetails.getStartTime());
        exam.setEndTime(examDetails.getEndTime());
        exam.setPassingMarks(examDetails.getPassingMarks());

        if (fromRankId != null) {
            Rank fromRank = rankRepository.findById(fromRankId)
                    .orElseThrow(() -> new RuntimeException("From Rank not found!"));
            exam.setFromRank(fromRank);
        }
        if (toRankId != null) {
            Rank toRank = rankRepository.findById(toRankId)
                    .orElseThrow(() -> new RuntimeException("To Rank not found!"));
            exam.setToRank(toRank);
        }

        exam.setUpdatedAt(LocalDateTime.now());
        return examRepository.save(exam);
    }

    @Transactional
    public Exam publishExam(Long id) {
        Exam exam = getExamById(id);
        if (exam == null) {
            throw new RuntimeException("Exam not found!");
        }
        exam.setStatus("PUBLISHED");
        exam.setUpdatedAt(LocalDateTime.now());
        return examRepository.save(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        examRepository.deleteById(id);
    }

    public boolean isExamAvailableForEmployee(Exam exam, Employee employee) {
        if (!"PUBLISHED".equals(exam.getStatus())) return false;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            return false;
        }

        if (employee.getRank() != null && exam.getFromRank() != null && exam.getToRank() != null) {
            int empPriority = employee.getRank().getPriority();
            int fromPriority = exam.getFromRank().getPriority();
            int toPriority = exam.getToRank().getPriority();
            return empPriority >= fromPriority && empPriority <= toPriority;
        }

        return false;
    }
}