package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.AssignmentSubmission;
import com.lms.lms_backend.entity.CourseAssignment;
import com.lms.lms_backend.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static com.lms.lms_backend.controller.ClassroomResponses.fail;
import static com.lms.lms_backend.controller.ClassroomResponses.ok;

/** Assignments (instructor creates, students submit) and submission grading. */
@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "http://localhost:3000")
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) {
        this.service = service;
    }

    // ---------------- instructor: assignments ----------------

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam Long teacherId,
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String instructions,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) Integer maxMarks,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment) {
        try {
            CourseAssignment a = service.create(teacherId, courseId, title, instructions, deadline, maxMarks, attachment);
            return ok("message", "Assignment created. Enrolled students can see it now.", "item", service.toMap(a));
        } catch (Exception e) {
            return fail(e);
        }
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String instructions,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) Integer maxMarks,
            @RequestParam(defaultValue = "false") boolean removeAttachment,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment) {
        try {
            CourseAssignment a = service.update(id, teacherId, title, instructions, deadline, maxMarks, attachment, removeAttachment);
            return ok("message", "Assignment updated.", "item", service.toMap(a));
        } catch (Exception e) {
            return fail(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id, @RequestParam Long teacherId) {
        try {
            service.delete(id, teacherId);
            return ok("message", "Assignment and its submissions were deleted.");
        } catch (Exception e) {
            return fail(e);
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> forTeacher(@PathVariable Long teacherId, @RequestParam(required = false) Long courseId) {
        try {
            return ResponseEntity.ok(service.forTeacher(teacherId, courseId));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // ---------------- student ----------------

    // Assignments of all enrolled courses, each with the student's own submission
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> forStudent(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(service.forStudent(studentId));
        } catch (Exception e) {
            return fail(e);
        }
    }

    @GetMapping("/student/{studentId}/pending-count")
    public ResponseEntity<?> pendingCount(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(ClassroomResponses.plain("pending", service.pendingCount(studentId)));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Submit (or re-submit before grading): a file and/or a GitHub / Google Drive link
    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submit(
            @PathVariable Long id,
            @RequestParam Long studentId,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) String note,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            AssignmentSubmission s = service.submit(id, studentId, file, link, note);
            return ok("message", "Assignment submitted.", "item", service.submissionMap(s, null));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // ---------------- instructor: submissions ----------------

    @GetMapping("/submissions/teacher/{teacherId}")
    public ResponseEntity<?> submissions(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(service.submissionsForTeacher(teacherId, courseId, assignmentId, status));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Marks / grade / feedback / status
    @PutMapping("/submissions/{submissionId}/review")
    public ResponseEntity<Map<String, Object>> review(@PathVariable Long submissionId, @RequestBody Map<String, Object> body) {
        try {
            Long teacherId = body.get("teacherId") == null ? null : Long.valueOf(body.get("teacherId").toString());
            Double marks = body.get("marks") == null || body.get("marks").toString().isBlank()
                    ? null : Double.valueOf(body.get("marks").toString());
            String grade = body.get("grade") == null ? null : body.get("grade").toString();
            String feedback = body.get("feedback") == null ? null : body.get("feedback").toString();
            String status = body.get("status") == null ? null : body.get("status").toString();
            AssignmentSubmission s = service.review(submissionId, teacherId, marks, grade, feedback, status);
            return ok("message", "Review saved.", "item", service.submissionMap(s, null));
        } catch (Exception e) {
            return fail(e);
        }
    }
}
