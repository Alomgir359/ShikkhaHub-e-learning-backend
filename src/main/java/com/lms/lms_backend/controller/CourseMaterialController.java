package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.CourseMaterial;
import com.lms.lms_backend.service.CourseMaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class CourseMaterialController {

    private final CourseMaterialService materialService;

    public CourseMaterialController(CourseMaterialService materialService) {
        this.materialService = materialService;
    }

    // Upload PDF file
    @PostMapping("/upload/pdf")
    public ResponseEntity<Map<String, Object>> uploadPDF(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") String courseId,
            @RequestParam("courseTitle") String courseTitle,
            @RequestParam("teacherId") String teacherId,
            @RequestParam("teacherName") String teacherName,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        Map<String, Object> response = new HashMap<>();
        try {
            Long parsedCourseId = Long.parseLong(courseId);
            Long parsedTeacherId = Long.parseLong(teacherId);

            String fileUrl = materialService.savePDFFile(file, parsedCourseId, parsedTeacherId);

            CourseMaterial material = new CourseMaterial();
            material.setCourseId(parsedCourseId);
            material.setCourseTitle(courseTitle);
            material.setTeacherId(parsedTeacherId);
            material.setTeacherName(teacherName);
            material.setType("PDF");
            material.setTitle(title);
            material.setDescription(description);
            material.setFileUrl(fileUrl);
            material.setUploadedAt(LocalDateTime.now());

            CourseMaterial saved = materialService.saveMaterial(material);

            response.put("success", true);
            response.put("message", "PDF uploaded successfully!");
            response.put("material", saved);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Invalid ID format: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Upload Video (URL)
    @PostMapping("/upload/video")
    public ResponseEntity<Map<String, Object>> uploadVideo(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long teacherId = Long.valueOf(request.get("teacherId").toString());

            CourseMaterial material = new CourseMaterial();
            material.setCourseId(courseId);
            material.setCourseTitle((String) request.get("courseTitle"));
            material.setTeacherId(teacherId);
            material.setTeacherName((String) request.get("teacherName"));
            material.setType("VIDEO");
            material.setTitle((String) request.get("title"));
            material.setDescription((String) request.get("description"));
            material.setVideoUrl((String) request.get("videoUrl"));
            material.setUploadedAt(LocalDateTime.now());

            CourseMaterial saved = materialService.saveMaterial(material);

            response.put("success", true);
            response.put("message", "Video added successfully!");
            response.put("material", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Upload Assignment
    @PostMapping("/upload/assignment")
    public ResponseEntity<Map<String, Object>> uploadAssignment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long teacherId = Long.valueOf(request.get("teacherId").toString());

            CourseMaterial material = new CourseMaterial();
            material.setCourseId(courseId);
            material.setCourseTitle((String) request.get("courseTitle"));
            material.setTeacherId(teacherId);
            material.setTeacherName((String) request.get("teacherName"));
            material.setType("ASSIGNMENT");
            material.setTitle((String) request.get("title"));
            material.setDescription((String) request.get("description"));
            material.setAssignmentDetails((String) request.get("assignmentDetails"));
            if (request.containsKey("dueDate") && request.get("dueDate") != null && !request.get("dueDate").toString().isEmpty()) {
                material.setDueDate(LocalDateTime.parse(request.get("dueDate").toString()));
            }
            material.setUploadedAt(LocalDateTime.now());

            CourseMaterial saved = materialService.saveMaterial(material);

            response.put("success", true);
            response.put("message", "Assignment uploaded successfully!");
            response.put("material", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Get materials by course ID
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseMaterial>> getMaterialsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(materialService.getMaterialsByCourseId(courseId));
    }

    // Get materials by teacher ID
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<CourseMaterial>> getMaterialsByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(materialService.getMaterialsByTeacherId(teacherId));
    }

    // Get material by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMaterialById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<CourseMaterial> material = materialService.getMaterialById(id);
        if (material.isPresent()) {
            response.put("success", true);
            response.put("material", material.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Material not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Update material
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateMaterial(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<CourseMaterial> optionalMaterial = materialService.getMaterialById(id);
            if (!optionalMaterial.isPresent()) {
                response.put("success", false);
                response.put("message", "Material not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            CourseMaterial existingMaterial = optionalMaterial.get();

            // Update common fields
            if (request.containsKey("title")) {
                existingMaterial.setTitle((String) request.get("title"));
            }
            if (request.containsKey("description")) {
                existingMaterial.setDescription((String) request.get("description"));
            }

            // Update type-specific fields
            if (existingMaterial.getType().equals("ASSIGNMENT")) {
                if (request.containsKey("assignmentDetails")) {
                    existingMaterial.setAssignmentDetails((String) request.get("assignmentDetails"));
                }
                if (request.containsKey("dueDate") && request.get("dueDate") != null && !request.get("dueDate").toString().isEmpty()) {
                    existingMaterial.setDueDate(LocalDateTime.parse(request.get("dueDate").toString()));
                } else if (request.containsKey("dueDate") && request.get("dueDate") == null) {
                    existingMaterial.setDueDate(null);
                }
            } else if (existingMaterial.getType().equals("VIDEO")) {
                if (request.containsKey("videoUrl")) {
                    existingMaterial.setVideoUrl((String) request.get("videoUrl"));
                }
            }
            // For PDF, only title and description can be updated (file cannot be changed)

            CourseMaterial updated = materialService.saveMaterial(existingMaterial);

            response.put("success", true);
            response.put("message", "Material updated successfully!");
            response.put("material", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Delete material
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMaterial(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            materialService.deleteMaterial(id);
            response.put("message", "Material deleted successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}