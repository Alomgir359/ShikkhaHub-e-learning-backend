package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {
    List<CourseAssignment> findByCourseIdInOrderByCreatedAtDesc(Collection<Long> courseIds);
    List<CourseAssignment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<CourseAssignment> findByCourseId(Long courseId);
    long countByCourseId(Long courseId);
}
