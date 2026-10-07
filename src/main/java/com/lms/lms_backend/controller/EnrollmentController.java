package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.Enrollment;
import com.lms.lms_backend.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Manual bKash / Nagad enrollment.
     * Body: courseId, fullName, phone, email, paymentMethod (BKASH|NAGAD), transactionId,
     *       password (new account only) or studentId (already logged-in student).
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitEnrollment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = enrollmentService.submitPaymentEnrollment(request);
            response.putAll(result);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (EnrollmentService.EnrollmentException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            HttpStatus status = switch (e.getCode()) {
                case "ACCOUNT_EXISTS", "ALREADY_PENDING", "ALREADY_ENROLLED", "DUPLICATE_TRX", "EMAIL_IN_USE" -> HttpStatus.CONFLICT;
                case "WRONG_PASSWORD" -> HttpStatus.UNAUTHORIZED;
                default -> HttpStatus.BAD_REQUEST;
            };
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("code", "SERVER_ERROR");
            response.put("message", "কিছু একটা সমস্যা হয়েছে। একটু পরে আবার চেষ্টা করুন।");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Legacy create (paid enrollments are saved as PENDING until an admin verifies them)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createEnrollment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudentId(((Number) request.get("studentId")).longValue());
            enrollment.setCourseId(((Number) request.get("courseId")).longValue());
            enrollment.setStudentName((String) request.get("studentName"));
            enrollment.setStudentEmail((String) request.get("studentEmail"));
            enrollment.setStudentPhone((String) request.get("studentPhone"));
            enrollment.setCourseTitle((String) request.get("courseTitle"));
            enrollment.setCourseCode((String) request.get("courseCode"));
            enrollment.setAmount(request.get("amount") == null ? 0.0 : ((Number) request.get("amount")).doubleValue());
            enrollment.setPaymentMethod((String) request.get("paymentMethod"));
            enrollment.setTransactionId((String) request.get("transactionId"));

            Enrollment savedEnrollment = enrollmentService.createEnrollment(enrollment);

            response.put("success", true);
            response.put("message", "PENDING".equals(savedEnrollment.getStatus())
                    ? "Enrollment submitted. Waiting for payment verification."
                    : "Enrollment successful!");
            response.put("enrollment", savedEnrollment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Get enrollments by student ID (all statuses — the dashboard shows pending ones separately)
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrollment>> getEnrollmentsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudentId(studentId));
    }

    // Get enrollments by course ID
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrollment>> getEnrollmentsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourseId(courseId));
    }

    // enrolled = verified (ACTIVE/COMPLETED); status = latest status (PENDING / REJECTED / ACTIVE / null)
    @GetMapping("/check/{studentId}/{courseId}")
    public ResponseEntity<Map<String, Object>> checkEnrollment(@PathVariable Long studentId, @PathVariable Long courseId) {
        Map<String, Object> response = new HashMap<>();
        Enrollment latest = enrollmentService.getLatestEnrollment(studentId, courseId);
        response.put("enrolled", enrollmentService.isStudentEnrolled(studentId, courseId));
        response.put("status", latest == null ? null : (latest.getStatus() == null ? "ACTIVE" : latest.getStatus()));
        response.put("adminNote", latest == null ? null : latest.getAdminNote());
        return ResponseEntity.ok(response);
    }

    // Get all enrollments
    @GetMapping("/all")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
}
