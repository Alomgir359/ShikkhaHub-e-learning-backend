package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {
    List<CourseMaterial> findByCourseId(Long courseId);
    List<CourseMaterial> findByTeacherId(Long teacherId);
    List<CourseMaterial> findByType(String type);
}