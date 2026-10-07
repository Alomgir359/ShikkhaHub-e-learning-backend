package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // Safe version: old data may contain more than one row for the same student + course
    List<Enrollment> findAllByStudentIdAndCourseIdOrderByUpdatedAtDesc(Long studentId, Long courseId);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, String status);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // ===== Manual bKash / Nagad payment verification =====
    boolean existsByTransactionIdIgnoreCase(String transactionId);

    List<Enrollment> findAllByOrderByEnrollmentDateDesc();

    List<Enrollment> findByStatusOrderByEnrollmentDateDesc(String status);

    long countByStatus(String status);

    Optional<Enrollment> findFirstByStudentIdAndStatusOrderByUpdatedAtDesc(Long studentId, String status);

    long countByStudentIdAndStatusIn(Long studentId, List<String> statuses);
}
