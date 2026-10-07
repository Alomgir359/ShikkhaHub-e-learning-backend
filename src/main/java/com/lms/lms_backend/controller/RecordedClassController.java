package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.RecordedClass;
import com.lms.lms_backend.service.RecordedClassService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static com.lms.lms_backend.controller.ClassroomResponses.fail;
import static com.lms.lms_backend.controller.ClassroomResponses.ok;

/** Recorded classes: the instructor uploads, enrolled students watch. */
@RestController
@RequestMapping("/api/recorded-classes")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class RecordedClassController {

    private final RecordedClassService service;

    public RecordedClassController(RecordedClassService service) {
        this.service = service;
    }

    // Instructor: upload a recorded class (video file OR video link, optional thumbnail)
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam Long teacherId,
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer classNumber,
            @RequestParam(required = false) Integer durationSeconds,
            @RequestParam(required = false) String videoUrl,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            RecordedClass rc = service.create(teacherId, courseId, title, description, classNumber,
                    durationSeconds, videoUrl, video, thumbnail);
            return ok("message", "Recorded class uploaded. Enrolled students can watch it now.", "item", service.toMap(rc));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Instructor: edit details / replace video or thumbnail (POST, because browsers send multipart forms most reliably this way)
    @PostMapping("/{id}/update")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer classNumber,
            @RequestParam(required = false) Integer durationSeconds,
            @RequestParam(required = false) String videoUrl,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            RecordedClass rc = service.update(id, teacherId, title, description, classNumber,
                    durationSeconds, videoUrl, video, thumbnail);
            return ok("message", "Recorded class updated.", "item", service.toMap(rc));
        } catch (Exception e) {
            return fail(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id, @RequestParam Long teacherId) {
        try {
            service.delete(id, teacherId);
            return ok("message", "Recorded class deleted.");
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Instructor: everything this instructor uploaded (optionally one course)
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> forTeacher(@PathVariable Long teacherId, @RequestParam(required = false) Long courseId) {
        try {
            return ResponseEntity.ok(service.forTeacher(teacherId, courseId));
        } catch (Exception e) {
            return fail(e);
        }
    }

    // Student: recorded classes of all enrolled courses (optionally one course)
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> forStudent(@PathVariable Long studentId, @RequestParam(required = false) Long courseId) {
        try {
            return ResponseEntity.ok(service.forStudent(studentId, courseId));
        } catch (Exception e) {
            return fail(e);
        }
    }
}
