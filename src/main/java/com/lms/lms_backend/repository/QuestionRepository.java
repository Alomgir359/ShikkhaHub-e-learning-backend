package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Exam;
import com.lms.lms_backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamOrderByOrderNumberAsc(Exam exam);
    List<Question> findByExamAndQuestionType(Exam exam, String questionType);
    void deleteByExam(Exam exam);
    long countByExam(Exam exam);
}