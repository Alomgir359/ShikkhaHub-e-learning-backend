package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.LiveClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {
    List<LiveClass> findByCourseIdInAndClassDateGreaterThanEqualOrderByClassDateAscStartTimeAsc(
            Collection<Long> courseIds, LocalDate from);
    List<LiveClass> findByTeacherIdOrderByClassDateDescStartTimeDesc(Long teacherId);
    long countByCourseId(Long courseId);
}
