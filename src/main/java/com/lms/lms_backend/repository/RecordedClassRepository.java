package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.RecordedClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RecordedClassRepository extends JpaRepository<RecordedClass, Long> {
    List<RecordedClass> findByCourseIdInOrderByUploadedAtDesc(Collection<Long> courseIds);
    List<RecordedClass> findByTeacherIdOrderByUploadedAtDesc(Long teacherId);
    List<RecordedClass> findByCourseId(Long courseId);
    long countByCourseId(Long courseId);
}
