package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.Enrollment;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** Summary numbers + course lists for the student and instructor dashboards. */
@Service
public class DashboardService {

    private final CourseAccessService access;
    private final EnrollmentRepository enrollments;
    private final TeacherRepository teachers;
    private final RecordedClassRepository recorded;
    private final CourseAssignmentRepository assignmentRepo;
    private final LiveClassRepository liveRepo;
    private final AssignmentService assignmentService;
    private final LiveClassService liveService;

    public DashboardService(CourseAccessService access, EnrollmentRepository enrollments, TeacherRepository teachers,
                            RecordedClassRepository recorded, CourseAssignmentRepository assignmentRepo,
                            LiveClassRepository liveRepo, AssignmentService assignmentService,
                            LiveClassService liveService) {
        this.access = access;
        this.enrollments = enrollments;
        this.teachers = teachers;
        this.recorded = recorded;
        this.assignmentRepo = assignmentRepo;
        this.liveRepo = liveRepo;
        this.assignmentService = assignmentService;
        this.liveService = liveService;
    }

    // ---------------- student ----------------

    /** Verified-enrolled courses of the student (one query per course, no extra round trips from the browser). */
    public List<Map<String, Object>> studentCourses(Long studentId) {
        access.requireStudent(studentId);
        Map<Long, Enrollment> byCourse = new LinkedHashMap<>();
        for (Enrollment e : enrollments.findByStudentId(studentId)) {
            if (CourseAccessService.isVerified(e)) byCourse.putIfAbsent(e.getCourseId(), e);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<Long, Enrollment> en : byCourse.entrySet()) {
            Course c = access.requireCourseOrNull(en.getKey());
            if (c == null) continue;
            Map<String, Object> m = courseMap(c);
            m.put("enrolledAt", en.getValue().getEnrollmentDate());
            m.put("enrollmentStatus", en.getValue().getStatus() == null ? "ACTIVE" : en.getValue().getStatus());
            m.put("recordedCount", recorded.countByCourseId(c.getId()));
            out.add(m);
        }
        return out;
    }

    // ---------------- instructor ----------------

    public Map<String, Object> instructorSummary(Long teacherId, LocalDate today) {
        access.requireTeacher(teacherId);
        Set<Long> courseIds = access.courseIdsOfTeacher(teacherId);
        Set<Long> students = new HashSet<>();
        for (Long cid : courseIds) {
            for (Enrollment e : access.verifiedEnrollments(cid)) students.add(e.getStudentId());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assignedCourses", courseIds.size());
        m.put("totalStudents", students.size());
        m.put("activeAssignments", assignmentService.activeCount(teacherId));
        m.put("upcomingLiveClasses", liveService.countUpcoming(teacherId, today));
        m.put("toReview", assignmentService.submissionsForTeacher(teacherId, null, null, "SUBMITTED").size());
        m.put("recentSubmissions", assignmentService.recentSubmissions(teacherId, 8));
        return m;
    }

    public List<Map<String, Object>> instructorCourses(Long teacherId) {
        access.requireTeacher(teacherId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Course c : access.coursesOfTeacher(teacherId)) {
            Map<String, Object> m = courseMap(c);
            m.put("studentCount", access.verifiedEnrollments(c.getId()).size());
            m.put("recordedCount", recorded.countByCourseId(c.getId()));
            m.put("assignmentCount", assignmentRepo.countByCourseId(c.getId()));
            m.put("liveCount", liveRepo.countByCourseId(c.getId()));
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> courseStudents(Long teacherId, Long courseId) {
        access.requireOwnedCourse(teacherId, courseId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Enrollment e : access.verifiedEnrollments(courseId)) {
            Teacher t = teachers.findById(e.getStudentId()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", e.getStudentId());
            m.put("name", t != null ? t.getFullName() : e.getStudentName());
            m.put("email", t != null ? t.getEmail() : e.getStudentEmail());
            m.put("phone", t != null && t.getPhone() != null ? t.getPhone() : e.getStudentPhone());
            m.put("enrolledAt", e.getEnrollmentDate());
            m.put("status", e.getStatus() == null ? "ACTIVE" : e.getStatus());
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> courseMap(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("courseTitle", c.getCourseTitle());
        m.put("subTitle", c.getSubTitle());
        m.put("courseType", c.getCourseType());
        m.put("level", c.getLevel());
        m.put("category", c.getCategory());
        m.put("instructorName", c.getInstructorName());
        m.put("thumbnailUrl", c.getThumbnailUrl());
        m.put("durationInWeeks", c.getDurationInWeeks());
        m.put("batchNumber", c.getBatchNumber());
        m.put("isApproved", c.getIsApproved());
        m.put("isPublished", c.getIsPublished());
        return m;
    }
}
