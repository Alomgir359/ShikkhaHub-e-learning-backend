
package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.Admin;
import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.service.AdminService;
import com.lms.lms_backend.service.CourseService;
import com.lms.lms_backend.service.EnrollmentService;
import com.lms.lms_backend.service.TeacherService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TeacherService teacherService;
    private final AdminService adminService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    // File upload directory
    private final String uploadDirectory = "uploads/teachers/";

    public AdminController(TeacherService teacherService, AdminService adminService, CourseService courseService,
                           EnrollmentService enrollmentService) {
        this.teacherService = teacherService;
        this.adminService = adminService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        // Create default admin if not exists
        adminService.createDefaultAdmin();
    }
    // Add this method to AdminController.java to show the exam management menu in sidebar
    @GetMapping("/exam-menu")
    public String examMenu(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        return "redirect:/admin/exam/dashboard";
    }
    // Admin Login Page
    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session) {
        if (session.getAttribute("admin") != null) {
            return "redirect:/admin/teachers";
        }
        return "admin-login";
    }

    // Teacher Details Page
    @GetMapping("/teacher-details/{id}")
    public String teacherDetails(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        Teacher teacher = teacherService.getTeacherById(id);
        model.addAttribute("teacher", teacher);
        return "teacher-details";
    }

    // Admin Login Submit
    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {
        Admin admin = adminService.login(username, password);
        if (admin != null) {
            session.setAttribute("admin", admin);
            return "redirect:/admin/teachers";
        } else {
            model.addAttribute("error", "Invalid username or password!");
            return "admin-login";
        }
    }

    // Admin Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    // Teacher Dashboard (Protected)
    @GetMapping("/teachers")
    public String teacherDashboard(Model model, HttpSession session) {
        // Check if admin is logged in
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        List<Teacher> allTeachers = teacherService.getAllTeachers();
        List<Teacher> pendingTeachers = teacherService.getPendingTeachers();

        model.addAttribute("allTeachers", allTeachers);
        model.addAttribute("pendingTeachers", pendingTeachers);
        model.addAttribute("teacherCount", teacherService.getTotalTeacherCount());
        model.addAttribute("pendingCount", teacherService.getPendingTeacherCount());
        model.addAttribute("approvedCount", teacherService.getApprovedTeacherCount());
        model.addAttribute("rejectedCount", teacherService.getRejectedTeacherCount());
        model.addAttribute("admin", session.getAttribute("admin"));

        // Course data
        model.addAttribute("pendingCourses", courseService.getPendingApprovalCourses());
        model.addAttribute("publishRequestCourses", courseService.getPublishRequestCourses());
        model.addAttribute("pendingCoursesCount", courseService.getPendingCoursesCount());
        model.addAttribute("publishRequestCount", courseService.getPublishRequestCount());
        model.addAttribute("allCourses", courseService.getAllCourses());

        // Enrollment (bKash / Nagad) requests waiting for verification
        model.addAttribute("pendingEnrollmentCount", enrollmentService.countByStatus(EnrollmentService.PENDING));

        return "teacher-approval";
    }

    // Course Details Page (Admin View)
    @GetMapping("/course-details/{id}")
    public String courseDetails(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        model.addAttribute("admin", session.getAttribute("admin"));
        return "course-details-admin";
    }

    // ==================== UPDATED COURSE APPROVAL WITH PRICE ====================

    /**
     * Approve course with price - POST endpoint for form submission from modal
     * This properly handles the price parameter and sets it in the database
     */
    @PostMapping("/course/approve/{id}")
    public String approveCourseWithPrice(@PathVariable Long id,
                                         @RequestParam Double price,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                redirectAttributes.addFlashAttribute("error", "Course not found");
                return "redirect:/admin/teachers";
            }

            // Check if already approved
            if (course.getIsApproved() != null && course.getIsApproved() == 1) {
                redirectAttributes.addFlashAttribute("error", "Course is already approved!");
                return "redirect:/admin/teachers";
            }

            // Validate price
            if (price == null || price < 0) {
                redirectAttributes.addFlashAttribute("error", "Please provide a valid price (0 or greater)");
                return "redirect:/admin/teachers";
            }

            // Approve the course and set the price
            course.setIsApproved(1);
            course.setPrice(price);
            course.setUpdatedAt(LocalDateTime.now());

            courseService.updateCourse(course);

            redirectAttributes.addFlashAttribute("success", "course_approved");
            redirectAttributes.addFlashAttribute("message", "Course approved successfully! Price set to ৳" + price);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to approve course: " + e.getMessage());
        }

        return "redirect:/admin/teachers";
    }

    /**
     * Simple approve course without price (for backward compatibility)
     * This will redirect to the form or show an error message
     */
    @GetMapping("/course/approve/{id}")
    public String approveCourseGet(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        redirectAttributes.addFlashAttribute("error", "Please use the approval form to set a price for this course");
        return "redirect:/admin/teachers";
    }

    // Publish Course - POST endpoint
    @PostMapping("/course/publish/{id}")
    public String publishCourse(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                redirectAttributes.addFlashAttribute("error", "Course not found");
                return "redirect:/admin/teachers";
            }

            // Check if course is approved
            if (course.getIsApproved() == null || course.getIsApproved() != 1) {
                redirectAttributes.addFlashAttribute("error", "Course must be approved before publishing");
                return "redirect:/admin/teachers";
            }

            // Check if already published
            if (course.getIsPublished() != null && course.getIsPublished() == 1) {
                redirectAttributes.addFlashAttribute("error", "Course is already published!");
                return "redirect:/admin/teachers";
            }

            courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("success", "course_published");
            redirectAttributes.addFlashAttribute("message", "Course published successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to publish course: " + e.getMessage());
        }

        return "redirect:/admin/teachers";
    }

    // Publish Course - GET endpoint (redirect to POST or show error)
    @GetMapping("/course/publish/{id}")
    public String publishCourseGet(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid request method for publishing");
        return "redirect:/admin/teachers";
    }

    // Approve Teacher
    @GetMapping("/approve/{id}")
    public String approveTeacher(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        try {
            teacherService.approveTeacher(id);
            redirectAttributes.addFlashAttribute("success", "teacher_approved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    // Reject Teacher
    @GetMapping("/reject/{id}")
    public String rejectTeacher(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        try {
            teacherService.rejectTeacher(id);
            redirectAttributes.addFlashAttribute("success", "teacher_rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    // Delete Teacher
    @GetMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        try {
            teacherService.deleteTeacher(id);
            redirectAttributes.addFlashAttribute("success", "teacher_deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @GetMapping("/api/teacher/{id}")
    @ResponseBody
    public Teacher getTeacherApi(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return null;
        }
        return teacherService.getTeacherById(id);
    }

    // ==================== ফাইল সার্ভ করার জন্য এন্ডপয়েন্ট ====================

    // Serve uploaded files (CV, NID, Profile Picture, Organization ID)
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Create path to file
            Path filePath = Paths.get(uploadDirectory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // Check if file exists and is readable
            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // Return file as response
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Alternative endpoint for images (for direct viewing)
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDirectory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null || !contentType.startsWith("image/")) {
                    contentType = "image/jpeg";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}