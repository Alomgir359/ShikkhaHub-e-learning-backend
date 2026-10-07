package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.LiveClass;
import com.lms.lms_backend.service.LiveClassService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static com.lms.lms_backend.controller.ClassroomResponses.fail;
import static com.lms.lms_backend.controller.ClassroomResponses.ok;

/** Zoom live classes: the instructor schedules, enrolled students join. */
@RestController
@RequestMapping("/api/live-classes")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class LiveClassController {

    private final LiveClassService service;

    public LiveClassController(LiveClassService service) {
        this.service = service;
    }

    private static Long longOf(Object v) { return v == null || v.toString().isBlank() ? null : Long.valueOf(v.toString()); }
    private static String strOf(Object v) { return v == null ? null : v.toString(); }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> b) {
        try {
            LiveClass lc = service.create(longOf(b.get("teacherId")), longOf(b.get("courseId")), strOf(b.get("title")),
                    strOf(b.get("description")), strOf(b.get("classDate")), strOf(b.get("startTime")), strOf(b.get("meetingLink")));
            return ok("message", "Live class scheduled. Enrolled students can see it now.", "item", service.toMap(lc));
        } catch (Exception e) {
            return fail(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        try {
            LiveClass lc = service.update(id, longOf(b.get("teacherId")), strOf(b.get("title")), strOf(b.get("description")),
                    strOf(b.get("classDate")), strOf(b.get("startTime")), strOf(b.get("meetingLink")));
            return ok("message", "Live class updated.", "item", service.toMap(lc));
        } catch (Exception e) {
            return fail(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id, @RequestParam Long teacherId) {
        try {
            service.delete(id, teacherId);
            return ok("message", "Live class deleted.");
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

    // Student: live classes on/after `from` (yyyy-MM-dd, the browser's "today") for enrolled courses
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> forStudent(@PathVariable Long studentId, @RequestParam(required = false) String from) {
        try {
            LocalDate start;
            try {
                start = from == null || from.isBlank() ? LocalDate.now(ZoneId.of("Asia/Dhaka")) : LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                start = LocalDate.now(ZoneId.of("Asia/Dhaka"));
            }
            return ResponseEntity.ok(service.forStudent(studentId, start));
        } catch (Exception e) {
            return fail(e);
        }
    }
}
