package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.Enrollment;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.CourseRepository;
import com.lms.lms_backend.repository.EnrollmentRepository;
import com.lms.lms_backend.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** "Who may see / change what" rules shared by the recorded-class, assignment and live-class features. */
@Service
public class CourseAccessService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;

    public CourseAccessService(CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               TeacherRepository teacherRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
    }

    /** Verified enrollment = ACTIVE / COMPLETED (old rows without a status count as ACTIVE). */
    public static boolean isVerified(Enrollment e) {
        return e.getStatus() == null || "ACTIVE".equals(e.getStatus()) || "COMPLETED".equals(e.getStatus());
    }

    public Course requireCourse(Long courseId) {
        if (courseId == null) throw ClassroomException.badRequest("Please select a course.");
        return courseRepository.findById(courseId)
                .orElseThrow(() -> ClassroomException.notFound("Course not found."));
    }

    public Course requireCourseOrNull(Long courseId) {
        return courseId == null ? null : courseRepository.findById(courseId).orElse(null);
    }

    /** The instructor must be the one assigned to the course. */
    public Course requireOwnedCourse(Long teacherId, Long courseId) {
        Course course = requireCourse(courseId);
        if (teacherId == null || !teacherId.equals(course.getTeacherId())) {
            throw ClassroomException.forbidden("You are not the instructor of this course.");
        }
        return course;
    }

    public Teacher requireTeacher(Long teacherId) {
        if (teacherId == null) throw ClassroomException.badRequest("teacherId is required.");
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> ClassroomException.notFound("Instructor not found."));
    }

    public Teacher requireStudent(Long studentId) {
        if (studentId == null) throw ClassroomException.badRequest("studentId is required.");
        return teacherRepository.findById(studentId)
                .orElseThrow(() -> ClassroomException.notFound("Student not found."));
    }

    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.findAllByStudentIdAndCourseIdOrderByUpdatedAtDesc(studentId, courseId)
                .stream().anyMatch(CourseAccessService::isVerified);
    }

    public void requireEnrolled(Long studentId, Long courseId) {
        if (!isEnrolled(studentId, courseId)) {
            throw ClassroomException.forbidden("You are not enrolled in this course.");
        }
    }

    /** Course ids the student is verified-enrolled in. */
    public Set<Long> enrolledCourseIds(Long studentId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Enrollment e : enrollmentRepository.findByStudentId(studentId)) {
            if (isVerified(e)) ids.add(e.getCourseId());
        }
        return ids;
    }

    public List<Course> coursesOfTeacher(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }

    public Set<Long> courseIdsOfTeacher(Long teacherId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Course c : coursesOfTeacher(teacherId)) ids.add(c.getId());
        return ids;
    }

    /** Verified enrollments of one course (one row per student, newest first). */
    public List<Enrollment> verifiedEnrollments(Long courseId) {
        Set<Long> seen = new LinkedHashSet<>();
        return enrollmentRepository.findByCourseId(courseId).stream()
                .filter(CourseAccessService::isVerified)
                .sorted((a, b) -> {
                    if (a.getEnrollmentDate() == null) return 1;
                    if (b.getEnrollmentDate() == null) return -1;
                    return b.getEnrollmentDate().compareTo(a.getEnrollmentDate());
                })
                .filter(e -> seen.add(e.getStudentId()))
                .toList();
    }
}
