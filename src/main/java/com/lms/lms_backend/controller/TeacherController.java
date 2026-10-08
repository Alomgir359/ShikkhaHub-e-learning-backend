package com.lms.lms_backend.controller;

import com.lms.lms_backend.dto.TeacherRegistrationDTO;
import com.lms.lms_backend.entity.Enrollment;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.service.EnrollmentService;
import com.lms.lms_backend.service.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class TeacherController {

    private final TeacherService teacherService;
    private final EnrollmentService enrollmentService;

    public TeacherController(TeacherService teacherService, EnrollmentService enrollmentService) {
        this.teacherService = teacherService;
        this.enrollmentService = enrollmentService;
    }

    // Step 1: Save basic info and get temp ID
    @PostMapping("/apply/step1")
    public ResponseEntity<Map<String, Object>> applyTeacherStep1(@RequestBody Teacher teacher) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long tempId = teacherService.saveTeacherStep1(teacher);
            response.put("success", true);
            response.put("message", "Step 1 completed successfully!");
            response.put("tempId", tempId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Step 2: Upload files and complete registration
    @PostMapping("/apply/step2/{tempId}")
    public ResponseEntity<Map<String, String>> applyTeacherStep2(
            @PathVariable Long tempId,
            @RequestParam("cv") MultipartFile cv,
            @RequestParam("nidPhoto") MultipartFile nidPhoto,
            @RequestParam("profilePhoto") MultipartFile profilePhoto,
            @RequestParam("organizationIdCard") MultipartFile organizationIdCard) {

        Map<String, String> response = new HashMap<>();
        try {
            String result = teacherService.saveTeacherStep2(tempId, cv, nidPhoto, profilePhoto, organizationIdCard);
            response.put("message", result);
            if (result.equals("Application submitted successfully!")) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // GET all pending teachers
    @GetMapping("/pending")
    public ResponseEntity<List<Teacher>> getPendingTeachers() {
        return ResponseEntity.ok(teacherService.getPendingTeachers());
    }

    // GET all teachers
    @GetMapping("/all")
    public ResponseEntity<List<Teacher>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    // GET teacher by ID
    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getTeacherById(@PathVariable Long id) {
        Teacher teacher = teacherService.getTeacherById(id);
        if (teacher != null) {
            return ResponseEntity.ok(teacher);
        }
        return ResponseEntity.notFound().build();
    }

    // APPROVE TEACHER
    @PutMapping("/approve/{id}")
    public ResponseEntity<Map<String, String>> approveTeacher(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        String result = teacherService.approveTeacher(id);
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    // REJECT TEACHER
    @PutMapping("/reject/{id}")
    public ResponseEntity<Map<String, String>> rejectTeacher(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        String result = teacherService.rejectTeacher(id);
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    // DELETE TEACHER
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteTeacher(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        String result = teacherService.deleteTeacher(id);
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    // LOGIN endpoint
    // Correct password but not approved yet → 403 with a clear reason
    // (student waiting for payment verification / payment rejected / instructor under review).
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Teacher teacher) {
        Map<String, String> response = new HashMap<>();
        Teacher t = teacherService.findByEmailAndPassword(teacher.getEmail(), teacher.getPassword());

        if (t == null) {
            response.put("message", "Invalid email or password");
            response.put("status", "FAILED");
            response.put("code", "INVALID_CREDENTIALS");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Students can always log in (no admin approval for the account itself). Only their COURSE waits
        // for payment verification. Accounts created by the old flow (PENDING/REJECTED) are fixed here.
        if ("STUDENT".equals(t.getRole()) && !"APPROVED".equals(t.getStatus())) {
            t.setStatus("APPROVED");
            t = teacherService.save(t);
        }

        if ("APPROVED".equals(t.getStatus())) {
            response.put("message", "Login Success");
            response.put("status", t.getStatus());
            response.put("role", t.getRole());
            response.put("name", t.getFullName());
            response.put("email", t.getEmail());
            response.put("id", String.valueOf(t.getId()));
            return ResponseEntity.ok(response);
        }

        response.put("status", t.getStatus());
        response.put("role", t.getRole());
        response.put("name", t.getFullName());
        if ("STUDENT".equals(t.getRole()) && "PENDING".equals(t.getStatus())) {
            response.put("code", "ENROLLMENT_PENDING");
            response.put("message", "আপনার এনরোলমেন্ট সম্পন্ন হয়েছে। অ্যাডমিন আপনার পেমেন্ট যাচাই করে অ্যাপ্রুভ করলেই আপনি লগইন করতে পারবেন।");
        } else if ("STUDENT".equals(t.getRole()) && "REJECTED".equals(t.getStatus())) {
            Enrollment rejected = enrollmentService.getLatestRejected(t.getId());
            String reason = rejected != null && rejected.getAdminNote() != null ? rejected.getAdminNote() : "পেমেন্ট যাচাই করা যায়নি।";
            response.put("code", "ENROLLMENT_REJECTED");
            response.put("reason", reason);
            response.put("message", "আপনার পেমেন্ট অ্যাপ্রুভ হয়নি: " + reason
                    + " সঠিক Transaction ID দিয়ে কোর্স পেজ থেকে আবার এনরোল করুন।");
        } else if ("PENDING".equals(t.getStatus())) {
            response.put("code", "ACCOUNT_PENDING");
            response.put("message", "Your instructor application is under review. You can log in after admin approval.");
        } else {
            response.put("code", "ACCOUNT_REJECTED");
            response.put("message", "Your account is not approved. Please contact support.");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Get teacher by email
    @GetMapping("/email/{email}")
    public ResponseEntity<Teacher> getTeacherByEmail(@PathVariable String email) {
        Teacher teacher = teacherService.findByEmail(email);
        if (teacher != null) {
            return ResponseEntity.ok(teacher);
        }
        return ResponseEntity.notFound().build();
    }

    // Change password endpoint
    @PutMapping("/change-password/{id}")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            Teacher teacher = teacherService.getTeacherById(id);
            if (teacher != null) {
                String currentPassword = request.get("currentPassword");
                String newPassword = request.get("newPassword");

                if (teacher.getPassword().equals(currentPassword)) {
                    teacher.setPassword(newPassword);
                    teacherService.updateTeacher(teacher);
                    response.put("message", "Password changed successfully");
                    return ResponseEntity.ok(response);
                } else {
                    response.put("message", "Current password is incorrect");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            } else {
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ==================== নতুন এন্ডপয়েন্ট ====================

    // Update teacher profile (for students)
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, String>> updateTeacher(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            Teacher teacher = teacherService.getTeacherById(id);
            if (teacher != null) {
                if (request.containsKey("fullName")) {
                    teacher.setFullName(request.get("fullName"));
                }
                if (request.containsKey("email")) {
                    teacher.setEmail(request.get("email"));
                }
                if (request.containsKey("phone")) {
                    teacher.setPhone(request.get("phone"));
                }
                if (request.containsKey("currentProfession")) {
                    teacher.setCurrentProfession(request.get("currentProfession"));
                }
                if (request.containsKey("organization")) {
                    teacher.setOrganization(request.get("organization"));
                }
                if (request.containsKey("experience")) {
                    teacher.setExperience(request.get("experience"));
                }
                teacherService.updateTeacher(teacher);

                response.put("message", "Profile updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Upload profile picture
    @PostMapping("/upload-profile-pic/{id}")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("profilePhoto") MultipartFile profilePhoto) {
        Map<String, String> response = new HashMap<>();
        try {
            Teacher teacher = teacherService.getTeacherById(id);
            if (teacher != null) {
                String fileName = teacherService.saveProfilePicture(id, profilePhoto);
                teacher.setProfilePhotoPath(fileName);
                teacherService.updateTeacher(teacher);
                response.put("profilePhotoPath", fileName);
                response.put("message", "Profile picture uploaded successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Get profile picture
    @GetMapping("/profile-pic/{id}")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable Long id) {
        try {
            Teacher teacher = teacherService.getTeacherById(id);
            if (teacher != null && teacher.getProfilePhotoPath() != null) {
                byte[] imageBytes = teacherService.getProfilePicture(teacher.getProfilePhotoPath());
                return ResponseEntity.ok().body(imageBytes);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Count total teachers
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTeacherCount() {
        Map<String, Long> response = new HashMap<>();
        response.put("total", teacherService.getTotalTeacherCount());
        response.put("pending", teacherService.getPendingTeacherCount());
        response.put("approved", teacherService.getApprovedTeacherCount());
        return ResponseEntity.ok(response);
    }

    // Update student role and status
    @PutMapping("/update-role/{id}")
    public ResponseEntity<Map<String, String>> updateStudentRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            Teacher teacher = teacherService.getTeacherById(id);
            if (teacher != null) {
                teacher.setRole(request.get("role"));
                teacher.setStatus(request.get("status"));
                teacherService.updateTeacher(teacher);
                response.put("message", "Role updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Teacher not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}