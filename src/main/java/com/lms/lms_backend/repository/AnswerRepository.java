package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Answer;
import com.lms.lms_backend.entity.ExamAttempt;
import com.lms.lms_backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByExamAttempt(ExamAttempt examAttempt);
    Optional<Answer> findByExamAttemptAndQuestion(ExamAttempt examAttempt, Question question);
    void deleteByExamAttempt(ExamAttempt examAttempt);
}