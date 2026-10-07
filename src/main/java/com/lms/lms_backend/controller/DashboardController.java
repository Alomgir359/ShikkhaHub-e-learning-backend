package com.lms.lms_backend.controller;

import com.lms.lms_backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import static com.lms.lms_backend.controller.ClassroomResponses.fail;

/** Course lists and summary numbers for the student and instructor dashboards. */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    // Student: verified-enrolled courses
    @GetMapping("/student/{studentId}/courses")
    public ResponseEntity<?> studentCourses(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(service.studentCourses(studentId));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Instructor: counters + recent submissions
    @GetMapping("/instructor/{teacherId}/summary")
    public ResponseEntity<?> instructorSummary(@PathVariable Long teacherId, @RequestParam(required = false) String today) {
        try {
            LocalDate date;
            try {
                date = today == null || today.isBlank() ? LocalDate.now(ZoneId.of("Asia/Dhaka")) : LocalDate.parse(today);
            } catch (DateTimeParseException e) {
                date = LocalDate.now(ZoneId.of("Asia/Dhaka"));
            }
            return ResponseEntity.ok(service.instructorSummary(teacherId, date));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Instructor: assigned courses with student / recorded / assignment / live counters
    @GetMapping("/instructor/{teacherId}/courses")
    public ResponseEntity<?> instructorCourses(@PathVariable Long teacherId) {
        try {
            return ResponseEntity.ok(service.instructorCourses(teacherId));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Instructor: students enrolled in one of the instructor's courses
    @GetMapping("/instructor/{teacherId}/courses/{courseId}/students")
    public ResponseEntity<?> courseStudents(@PathVariable Long teacherId, @PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(service.courseStudents(teacherId, courseId));
        } catch (Exception e) {
            return fail(e);
        }
    }
}
