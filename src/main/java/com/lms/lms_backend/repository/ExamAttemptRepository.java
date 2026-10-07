package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Employee;
import com.lms.lms_backend.entity.Exam;
import com.lms.lms_backend.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByExamAndEmployeeAndStatus(Exam exam, Employee employee, String status);
    List<ExamAttempt> findByEmployee(Employee employee);
    List<ExamAttempt> findByExam(Exam exam);
    boolean existsByExamAndEmployee(Exam exam, Employee employee);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam = :exam AND ea.status = 'COMPLETED'")
    List<ExamAttempt> findCompletedAttemptsByExam(@Param("exam") Exam exam);

    @Query("SELECT AVG(ea.obtainedMarks) FROM ExamAttempt ea WHERE ea.exam = :exam AND ea.status = 'COMPLETED'")
    Double getAverageMarksForExam(@Param("exam") Exam exam);
}