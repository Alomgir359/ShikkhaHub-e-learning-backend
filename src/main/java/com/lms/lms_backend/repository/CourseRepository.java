//package com.lms.lms_backend.repository;
//
//import com.lms.lms_backend.entity.Course;
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.List;
//
//public interface CourseRepository extends JpaRepository<Course, Long> {
//    List<Course> findByTeacherId(Long teacherId);
//    List<Course> findByLevel(String level);
//    List<Course> findByCategory(String category);
//}

package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacherId(Long teacherId);
    List<Course> findByLevel(String level);
    List<Course> findByCategory(String category);

    // ========== QUERIES FOR PUBLISHED COURSES ONLY ==========

    // Get only published courses (isPublished = 1)
    @Query("SELECT c FROM Course c WHERE c.isPublished = 1")
    List<Course> findAllPublishedCourses();

    // Get published course by ID
    @Query("SELECT c FROM Course c WHERE c.id = :id AND c.isPublished = 1")
    Course findPublishedCourseById(@Param("id") Long id);

    // Get published courses by level
    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 AND c.level = :level")
    List<Course> findPublishedCoursesByLevel(@Param("level") String level);

    // Get published courses by category
    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 AND c.category = :category")
    List<Course> findPublishedCoursesByCategory(@Param("category") String category);

    // Search published courses by title or category
    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 AND (LOWER(c.courseTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchPublishedCourses(@Param("keyword") String keyword);

    // Get published courses by teacher (for published courses only)
    @Query("SELECT c FROM Course c WHERE c.teacherId = :teacherId AND c.isPublished = 1")
    List<Course> findPublishedCoursesByTeacherId(@Param("teacherId") Long teacherId);

    // ========== E-LEARNING QUERIES ==========
    // Old rows created before courseType existed have NULL type -> treated as LIVE

    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 " +
           "AND (c.courseType IS NULL OR UPPER(c.courseType) = 'LIVE') " +
           "AND (c.batchStartDate IS NULL OR c.batchStartDate >= :today) " +
           "ORDER BY c.batchStartDate ASC NULLS LAST")
    List<Course> findUpcomingLiveCourses(@Param("today") java.time.LocalDate today);

    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 AND (c.courseType IS NULL OR UPPER(c.courseType) = 'LIVE')")
    List<Course> findPublishedLiveCourses();

    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 AND UPPER(c.courseType) = :type")
    List<Course> findPublishedCoursesByType(@Param("type") String type);

    // LIVE + OFFLINE batches that have not started yet (or have no date), soonest first
    @Query("SELECT c FROM Course c WHERE c.isPublished = 1 " +
           "AND (c.courseType IS NULL OR UPPER(c.courseType) IN ('LIVE', 'OFFLINE')) " +
           "AND (c.batchStartDate IS NULL OR c.batchStartDate >= :today) " +
           "ORDER BY c.batchStartDate ASC NULLS LAST")
    List<Course> findUpcomingBatchCourses(@Param("today") java.time.LocalDate today);
}
