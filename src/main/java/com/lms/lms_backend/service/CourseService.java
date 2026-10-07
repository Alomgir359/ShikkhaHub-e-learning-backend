

package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.repository.CourseRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Course createCourse(Course course) {
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        course.setStatus("DRAFT");
        course.setEnrolledStudents(0);
        course.setTotalStudents(0);
        course.setRating(0.0);
        course.setTotalRatings(0);
        // Auto-generate unique enrollment key
        if (course.getEnrollmentKey() == null || course.getEnrollmentKey().isEmpty()) {
            String key = "EK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            course.setEnrollmentKey(key);
        }
        return courseRepository.save(course);
    }

    public List<Course> getCoursesByTeacherId(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }

    public Course getCourseById(Long id) {
        Optional<Course> course = courseRepository.findById(id);
        return course.orElse(null);
    }

    public Course updateCourse(Course course) {
        course.setUpdatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> searchCourses(String keyword) {
        return courseRepository.findAll().stream()
                .filter(course -> course.getCourseTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                        (course.getCategory() != null && course.getCategory().toLowerCase().contains(keyword.toLowerCase())))
                .collect(Collectors.toList());
    }

    public List<Course> getCoursesByLevel(String level) {
        return courseRepository.findByLevel(level);
    }

    public List<Course> getCoursesByCategory(String category) {
        return courseRepository.findByCategory(category);
    }

    public List<Course> getPendingApprovalCourses() {
        return courseRepository.findAll().stream()
                .filter(course -> course.getIsApproved() != null && course.getIsApproved() == 0)
                .collect(Collectors.toList());
    }

    public List<Course> getPublishRequestCourses() {
        return courseRepository.findAll().stream()
                .filter(course -> course.getIsApproved() != null && course.getIsApproved() == 1
                        && course.getIsPublished() != null && course.getIsPublished() == 0)
                .collect(Collectors.toList());
    }

    // UPDATED: Approve course with price
    public Course approveCourse(Long id, Double price) {
        Course course = getCourseById(id);
        if (course != null) {
            course.setIsApproved(1);
            if (price != null) {
                course.setPrice(price);
            }
            course.setUpdatedAt(LocalDateTime.now());
            return courseRepository.save(course);
        }
        return null;
    }

    // Overloaded method for backward compatibility (without price)
    public Course approveCourse(Long id) {
        return approveCourse(id, null);
    }

    public Course publishCourse(Long id) {
        Course course = getCourseById(id);
        if (course != null) {
            course.setIsPublished(1);
            course.setStatus("ACTIVE");
            course.setUpdatedAt(LocalDateTime.now());
            return courseRepository.save(course);
        }
        return null;
    }

    // Send request for publish (teacher clicks button)
    public Course sendPublishRequest(Long id) {
        Course course = getCourseById(id);
        if (course != null) {
            // Only if course is approved (isApproved == 1) and not already published
            if (course.getIsApproved() == 1 && (course.getIsPublished() == null || course.getIsPublished() != 1)) {
                course.setIsPublished(0); // 0 means requested for publish, pending admin approval
                course.setUpdatedAt(LocalDateTime.now());
                return courseRepository.save(course);
            }
        }
        return null;
    }

    public long getPendingCoursesCount() {
        return getPendingApprovalCourses().size();
    }

    public long getPublishRequestCount() {
        return getPublishRequestCourses().size();
    }

    // ========== METHODS FOR PUBLISHED COURSES ONLY ==========

    public List<Course> getAllPublishedCourses() {
        return courseRepository.findAllPublishedCourses();
    }

    public Course getPublishedCourseById(Long id) {
        return courseRepository.findPublishedCourseById(id);
    }

    public List<Course> searchPublishedCourses(String keyword) {
        return courseRepository.searchPublishedCourses(keyword);
    }

    public List<Course> getPublishedCoursesByLevel(String level) {
        return courseRepository.findPublishedCoursesByLevel(level);
    }

    public List<Course> getPublishedCoursesByCategory(String category) {
        return courseRepository.findPublishedCoursesByCategory(category);
    }

    public List<Course> getPublishedCoursesByTeacherId(Long teacherId) {
        return courseRepository.findPublishedCoursesByTeacherId(teacherId);
    }

    // ========== E-LEARNING: LIVE / RECORDED ==========

    // Published LIVE courses whose batch has not started yet (or has no date set), soonest first
    public List<Course> getUpcomingLiveCourses() {
        return courseRepository.findUpcomingLiveCourses(java.time.LocalDate.now());
    }

    public List<Course> getPublishedCoursesByType(String type) {
        String t = type == null ? "LIVE" : type.trim().toUpperCase();
        if ("LIVE".equals(t)) {
            return courseRepository.findPublishedLiveCourses();
        }
        return courseRepository.findPublishedCoursesByType(t);
    }

    // Published LIVE and OFFLINE batches that have not started yet
    public List<Course> getUpcomingBatchCourses() {
        return courseRepository.findUpcomingBatchCourses(java.time.LocalDate.now());
    }
}
