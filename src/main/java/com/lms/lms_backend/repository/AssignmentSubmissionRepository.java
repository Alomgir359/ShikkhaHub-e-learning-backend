package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<AssignmentSubmission> findByStudentId(Long studentId);
    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);
    List<AssignmentSubmission> findByAssignmentIdIn(Collection<Long> assignmentIds);
    List<AssignmentSubmission> findByCourseIdInOrderBySubmittedAtDesc(Collection<Long> courseIds);
}
