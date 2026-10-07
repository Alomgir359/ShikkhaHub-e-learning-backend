package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.service.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Student sign-up. Students are stored in the same "teachers" table with role = STUDENT
 * and status = APPROVED, so they can log in immediately through /api/teachers/login.
 * A student only sees a course in their dashboard after an enrollment (purchase) exists.
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class StudentAuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern BD_PHONE = Pattern.compile("^01[3-9]\\d{8}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");

    private final TeacherService teacherService;

    public StudentAuthController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> req) {
        Map<String, Object> res = new HashMap<>();
        String fullName = req.getOrDefault("fullName", "").trim();
        String email = req.getOrDefault("email", "").trim();
        String phone = req.getOrDefault("phone", "").trim();
        String password = req.getOrDefault("password", "");

        String error = null;
        if (fullName.length() < 2 || fullName.length() > 100) error = "Full name must be 2-100 characters.";
        else if (!EMAIL.matcher(email).matches()) error = "Please enter a valid email address.";
        else if (!BD_PHONE.matcher(phone).matches()) error = "Enter a valid Bangladeshi mobile number (01XXXXXXXXX).";
        else if (!PASSWORD.matcher(password).matches())
            error = "Password must be 8-64 characters with uppercase, lowercase, number and special character.";

        if (error != null) {
            res.put("success", false);
            res.put("message", error);
            return ResponseEntity.badRequest().body(res);
        }

        try {
            Teacher student = teacherService.registerStudent(fullName, email, phone, password);
            res.put("success", true);
            res.put("message", "Registration successful");
            res.put("id", String.valueOf(student.getId()));
            res.put("name", student.getFullName());
            res.put("email", student.getEmail());
            res.put("role", student.getRole());
            res.put("status", student.getStatus());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
        }
    }
}
