

package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class CourseController {

    private final CourseService courseService;
    private static final ObjectMapper JSON = new ObjectMapper();

    // ========== E-LEARNING FIELD HELPERS ==========
    // Accepts strings or numbers from the frontend; empty string clears the value.

    private static boolean has(Map<String, Object> req, String key) {
        return req != null && req.containsKey(key);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        String s = v.toString().trim();
        return s.isEmpty() ? null : (int) Double.parseDouble(s);
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        String s = v.toString().trim();
        return s.isEmpty() ? null : Double.parseDouble(s);
    }

    private static LocalDate toDate(Object v) {
        String s = str(v);
        if (s == null) return null;
        return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
    }

    private static LocalDateTime toDateTime(Object v) {
        String s = str(v);
        if (s == null) return null;
        if (s.length() == 10) return LocalDate.parse(s).atTime(23, 59);
        if (s.length() > 19) s = s.substring(0, 19);
        return LocalDateTime.parse(s);
    }

    private static void applyElearningFields(Course c, Map<String, Object> req) {
        if (has(req, "courseType")) {
            String type = str(req.get("courseType"));
            c.setCourseType(type == null ? "LIVE" : type.toUpperCase());
        }
        if (has(req, "batchNumber")) c.setBatchNumber(toInt(req.get("batchNumber")));
        if (has(req, "batchStartDate")) c.setBatchStartDate(toDate(req.get("batchStartDate")));
        if (has(req, "classDays")) c.setClassDays(str(req.get("classDays")));
        if (has(req, "classTime")) c.setClassTime(str(req.get("classTime")));
        if (has(req, "supportClassSchedule")) c.setSupportClassSchedule(str(req.get("supportClassSchedule")));
        if (has(req, "originalPrice")) c.setOriginalPrice(toDouble(req.get("originalPrice")));
        if (has(req, "offerEndsAt")) c.setOfferEndsAt(toDateTime(req.get("offerEndsAt")));
        if (has(req, "promoVideoUrl")) c.setPromoVideoUrl(str(req.get("promoVideoUrl")));
        if (has(req, "thumbnailUrl")) c.setThumbnailUrl(str(req.get("thumbnailUrl")));
        if (has(req, "totalClasses")) c.setTotalClasses(toInt(req.get("totalClasses")));
        if (has(req, "schedule")) c.setSchedule(str(req.get("schedule")));
        if (has(req, "language")) c.setLanguage(str(req.get("language")));
        if (has(req, "venue")) c.setVenue(str(req.get("venue")));
        if (has(req, "motivationalText")) c.setMotivationalText(str(req.get("motivationalText")));
    }

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCourse(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = new Course();

            course.setCourseTitle((String) request.get("courseTitle"));
            course.setSubTitle((String) request.get("subTitle"));
            course.setInstructorName((String) request.get("instructorName"));
            course.setInstructorExperience((String) request.get("instructorExperience"));
            course.setDurationInWeeks((Integer) request.get("durationInWeeks"));
            course.setPrice(((Number) request.get("price")).doubleValue());
            course.setTotalSeats((Integer) request.get("totalSeats"));
            course.setDescription((String) request.get("description"));
            course.setLevel((String) request.get("level"));
            course.setCategory((String) request.get("category"));
            course.setTeacherId(((Number) request.get("teacherId")).longValue());
            course.setTeacherEmail((String) request.get("teacherEmail"));

            if (request.containsKey("audience")) {
                course.setAudience((String) request.get("audience"));
            }
            if (request.containsKey("whatsIncluded")) {
                course.setWhatsIncluded((String) request.get("whatsIncluded"));
            }
            if (request.containsKey("schedule")) {
                course.setSchedule((String) request.get("schedule"));
            }
            if (request.containsKey("language")) {
                course.setLanguage((String) request.get("language"));
            }
            if (request.containsKey("thumbnailUrl")) {
                course.setThumbnailUrl((String) request.get("thumbnailUrl"));
            }
            if (request.containsKey("promoVideoUrl")) {
                course.setPromoVideoUrl((String) request.get("promoVideoUrl"));
            }
            if (request.containsKey("instructorBio")) {
                course.setInstructorBio((String) request.get("instructorBio"));
            }
            if (request.containsKey("instructorAvatar")) {
                course.setInstructorAvatar((String) request.get("instructorAvatar"));
            }
            if (request.containsKey("instructorTitle")) {
                course.setInstructorTitle((String) request.get("instructorTitle"));
            }
            if (request.containsKey("requirements")) {
                course.setRequirements((String) request.get("requirements"));
            }
            if (request.containsKey("learningOutcomes")) {
                course.setLearningOutcomes((String) request.get("learningOutcomes"));
            }

            Object weeklyModules = request.get("weeklyModules");
            if (weeklyModules != null) {
                course.setWeeklyModules(weeklyModules instanceof String
                        ? (String) weeklyModules
                        : JSON.writeValueAsString(weeklyModules));
            }

            // Batch / live / recorded / pricing / video info
            applyElearningFields(course, request);

            Course savedCourse = courseService.createCourse(course);

            response.put("success", true);
            response.put("message", "Course created successfully!");
            response.put("course", savedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Add this endpoint to verify enrollment key
    @PostMapping("/{id}/verify-key")
    public ResponseEntity<Map<String, Object>> verifyEnrollmentKey(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String enrollmentKey = request.get("enrollmentKey");

            if (enrollmentKey == null || enrollmentKey.trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "Please enter an enrollment key");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("valid", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Get the enrollment key from the course
            String courseEnrollmentKey = course.getEnrollmentKey();

            // If course doesn't have an enrollment key set, allow enrollment (for backward compatibility)
            if (courseEnrollmentKey == null || courseEnrollmentKey.isEmpty()) {
                response.put("valid", true);
                response.put("message", "No enrollment key required for this course");
                return ResponseEntity.ok(response);
            }

            // Verify the enrollment key (case-insensitive)
            if (courseEnrollmentKey.equalsIgnoreCase(enrollmentKey.trim())) {
                response.put("valid", true);
                response.put("message", "Enrollment key verified successfully!");
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                response.put("message", "Invalid enrollment key. Please check and try again.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("valid", false);
            response.put("message", "Error verifying enrollment key: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<Course>> getCoursesByTeacher(@PathVariable Long teacherId) {
        List<Course> courses = courseService.getCoursesByTeacherId(teacherId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            Course course = courseService.getCourseById(id);
            if (course != null) {
                return ResponseEntity.ok(course);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Course not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course existingCourse = courseService.getCourseById(id);
            if (existingCourse == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (request.containsKey("courseTitle")) {
                existingCourse.setCourseTitle((String) request.get("courseTitle"));
            }
            if (request.containsKey("subTitle")) {
                existingCourse.setSubTitle((String) request.get("subTitle"));
            }
            if (request.containsKey("description")) {
                existingCourse.setDescription((String) request.get("description"));
            }
            if (request.containsKey("durationInWeeks")) {
                existingCourse.setDurationInWeeks((Integer) request.get("durationInWeeks"));
            }
            if (request.containsKey("price")) {
                existingCourse.setPrice(((Number) request.get("price")).doubleValue());
            }
            if (request.containsKey("totalSeats")) {
                existingCourse.setTotalSeats((Integer) request.get("totalSeats"));
            }
            if (request.containsKey("level")) {
                existingCourse.setLevel((String) request.get("level"));
            }
            if (request.containsKey("category")) {
                existingCourse.setCategory((String) request.get("category"));
            }
            if (request.containsKey("audience")) {
                existingCourse.setAudience((String) request.get("audience"));
            }
            if (request.containsKey("whatsIncluded")) {
                existingCourse.setWhatsIncluded((String) request.get("whatsIncluded"));
            }
            if (request.containsKey("schedule")) {
                existingCourse.setSchedule((String) request.get("schedule"));
            }
            if (request.containsKey("language")) {
                existingCourse.setLanguage((String) request.get("language"));
            }
            if (request.containsKey("thumbnailUrl")) {
                existingCourse.setThumbnailUrl((String) request.get("thumbnailUrl"));
            }
            if (request.containsKey("promoVideoUrl")) {
                existingCourse.setPromoVideoUrl((String) request.get("promoVideoUrl"));
            }
            if (request.containsKey("totalStudents")) {
                existingCourse.setTotalStudents((Integer) request.get("totalStudents"));
            }
            if (request.containsKey("instructorBio")) {
                existingCourse.setInstructorBio((String) request.get("instructorBio"));
            }
            if (request.containsKey("instructorAvatar")) {
                existingCourse.setInstructorAvatar((String) request.get("instructorAvatar"));
            }
            if (request.containsKey("instructorTitle")) {
                existingCourse.setInstructorTitle((String) request.get("instructorTitle"));
            }
            if (request.containsKey("rating")) {
                existingCourse.setRating(((Number) request.get("rating")).doubleValue());
            }
            if (request.containsKey("totalRatings")) {
                existingCourse.setTotalRatings((Integer) request.get("totalRatings"));
            }
            if (request.containsKey("requirements")) {
                existingCourse.setRequirements((String) request.get("requirements"));
            }
            if (request.containsKey("learningOutcomes")) {
                existingCourse.setLearningOutcomes((String) request.get("learningOutcomes"));
            }
            if (request.containsKey("weeklyModules")) {
                existingCourse.setWeeklyModules((String) request.get("weeklyModules"));
            }

            applyElearningFields(existingCourse, request);
            existingCourse.setUpdatedAt(LocalDateTime.now());

            Course updatedCourse = courseService.updateCourse(existingCourse);

            response.put("success", true);
            response.put("message", "Course updated successfully!");
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> partialUpdateCourse(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course existingCourse = courseService.getCourseById(id);
            if (existingCourse == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            request.forEach((key, value) -> {
                switch (key) {
                    case "courseTitle": existingCourse.setCourseTitle((String) value); break;
                    case "subTitle": existingCourse.setSubTitle((String) value); break;
                    case "description": existingCourse.setDescription((String) value); break;
                    case "durationInWeeks": existingCourse.setDurationInWeeks((Integer) value); break;
                    case "price": existingCourse.setPrice(((Number) value).doubleValue()); break;
                    case "totalSeats": existingCourse.setTotalSeats((Integer) value); break;
                    case "level": existingCourse.setLevel((String) value); break;
                    case "category": existingCourse.setCategory((String) value); break;
                    case "audience": existingCourse.setAudience((String) value); break;
                    case "whatsIncluded": existingCourse.setWhatsIncluded((String) value); break;
                    case "schedule": existingCourse.setSchedule((String) value); break;
                    case "language": existingCourse.setLanguage((String) value); break;
                    case "thumbnailUrl": existingCourse.setThumbnailUrl((String) value); break;
                    case "promoVideoUrl": existingCourse.setPromoVideoUrl((String) value); break;
                    case "instructorBio": existingCourse.setInstructorBio((String) value); break;
                    case "instructorAvatar": existingCourse.setInstructorAvatar((String) value); break;
                    case "instructorTitle": existingCourse.setInstructorTitle((String) value); break;
                    case "requirements": existingCourse.setRequirements((String) value); break;
                    case "learningOutcomes": existingCourse.setLearningOutcomes((String) value); break;
                    case "weeklyModules": existingCourse.setWeeklyModules((String) value); break;
                    default: break;
                }
            });

            applyElearningFields(existingCourse, request);
            existingCourse.setUpdatedAt(LocalDateTime.now());

            Course updatedCourse = courseService.updateCourse(existingCourse);

            response.put("success", true);
            response.put("message", "Course updated successfully!");
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{id}/modules")
    public ResponseEntity<Map<String, Object>> addModule(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String weeklyModulesJson = (String) request.get("weeklyModules");
            course.setWeeklyModules(weeklyModulesJson);
            Course updatedCourse = courseService.updateCourse(course);

            response.put("success", true);
            response.put("message", "Module added successfully!");
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/{id}/modules")
    public ResponseEntity<Map<String, Object>> updateModules(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String weeklyModulesJson = (String) request.get("weeklyModules");
            course.setWeeklyModules(weeklyModulesJson);
            Course updatedCourse = courseService.updateCourse(course);

            response.put("success", true);
            response.put("message", "Modules updated successfully!");
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PatchMapping("/{id}/enroll")
    public ResponseEntity<Map<String, Object>> updateEnrolledStudents(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            int newEnrolledCount = (course.getEnrolledStudents() != null ? course.getEnrolledStudents() : 0) + 1;
            course.setEnrolledStudents(newEnrolledCount);
            course.setTotalStudents(newEnrolledCount);

            Course updatedCourse = courseService.updateCourse(course);

            response.put("success", true);
            response.put("message", "Enrollment count updated!");
            response.put("enrolledStudents", newEnrolledCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Send publish request endpoint
    @PostMapping("/{id}/send-publish-request")
    public ResponseEntity<Map<String, Object>> sendPublishRequest(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.sendPublishRequest(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found or cannot be published (not approved or already published)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            response.put("success", true);
            response.put("message", "Publish request sent successfully! Waiting for admin approval.");
            response.put("course", course);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ========== COURSE APPROVAL WITH PRICE ENDPOINT ==========
    // This endpoint handles admin approving a pending course with a price
    @PostMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveCourseWithPrice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Check if course is already approved
            if (course.getIsApproved() != null && course.getIsApproved() == 1) {
                response.put("success", false);
                response.put("message", "Course is already approved!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Get price from request body
            Double price = null;
            if (request.containsKey("price")) {
                Object priceObj = request.get("price");
                if (priceObj instanceof Integer) {
                    price = ((Integer) priceObj).doubleValue();
                } else if (priceObj instanceof Double) {
                    price = (Double) priceObj;
                } else if (priceObj instanceof String) {
                    try {
                        price = Double.parseDouble((String) priceObj);
                    } catch (NumberFormatException e) {
                        response.put("success", false);
                        response.put("message", "Invalid price format");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                }
            }

            if (price == null || price < 0) {
                response.put("success", false);
                response.put("message", "Please provide a valid price (0 or greater)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Approve the course and set the price
            course.setIsApproved(1);
            course.setPrice(price);
            course.setUpdatedAt(LocalDateTime.now());

            Course updatedCourse = courseService.updateCourse(course);

            response.put("success", true);
            response.put("message", "Course approved successfully! Price set to ৳" + price);
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Alternative: Simple approve without price (for backward compatibility)
    @GetMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveCourseSimple(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.approveCourse(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found or already approved");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("message", "Course approved successfully!");
            response.put("course", course);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Publish course endpoint (for admin to publish approved courses)
    @PostMapping("/publish/{id}")
    public ResponseEntity<Map<String, Object>> publishCourse(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Check if course is approved
            if (course.getIsApproved() == null || course.getIsApproved() != 1) {
                response.put("success", false);
                response.put("message", "Course must be approved before publishing");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Check if already published
            if (course.getIsPublished() != null && course.getIsPublished() == 1) {
                response.put("success", false);
                response.put("message", "Course is already published!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            course.setIsPublished(1);
            course.setStatus("ACTIVE");
            course.setUpdatedAt(LocalDateTime.now());

            Course updatedCourse = courseService.updateCourse(course);

            response.put("success", true);
            response.put("message", "Course published successfully! It is now visible to students.");
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCourse(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Course course = courseService.getCourseById(id);
            if (course == null) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            courseService.deleteCourse(id);
            response.put("success", true);
            response.put("message", "Course deleted successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ========== EXISTING ENDPOINTS (For admin/teacher use) ==========

    @GetMapping("/all")
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Course>> searchCourses(@RequestParam String keyword) {
        List<Course> courses = courseService.searchCourses(keyword);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<Course>> getCoursesByLevel(@PathVariable String level) {
        List<Course> courses = courseService.getCoursesByLevel(level);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Course>> getCoursesByCategory(@PathVariable String category) {
        List<Course> courses = courseService.getCoursesByCategory(category);
        return ResponseEntity.ok(courses);
    }

    // Get pending approval courses (for admin)
    @GetMapping("/pending-approval")
    public ResponseEntity<List<Course>> getPendingApprovalCourses() {
        List<Course> courses = courseService.getPendingApprovalCourses();
        return ResponseEntity.ok(courses);
    }

    // Get publish request courses (for admin)
    @GetMapping("/publish-requests")
    public ResponseEntity<List<Course>> getPublishRequestCourses() {
        List<Course> courses = courseService.getPublishRequestCourses();
        return ResponseEntity.ok(courses);
    }

    // ========== NEW ENDPOINTS FOR PUBLISHED COURSES ONLY (For Students) ==========

    // Get only published courses (isPublished == 1) - For students browsing courses
    @GetMapping("/published")
    public ResponseEntity<List<Course>> getPublishedCourses() {
        List<Course> courses = courseService.getAllPublishedCourses();
        return ResponseEntity.ok(courses);
    }

    // Upcoming LIVE batches (published, batch not started yet) - for the "Upcoming Live" page
    @GetMapping("/published/upcoming-live")
    public ResponseEntity<List<Course>> getUpcomingLiveCourses() {
        return ResponseEntity.ok(courseService.getUpcomingLiveCourses());
    }

    // Upcoming LIVE + OFFLINE batches together (Upcoming batches page)
    @GetMapping("/published/upcoming-batches")
    public ResponseEntity<List<Course>> getUpcomingBatchCourses() {
        return ResponseEntity.ok(courseService.getUpcomingBatchCourses());
    }

    // Get published courses by type: LIVE, OFFLINE or RECORDED
    @GetMapping("/published/type/{type}")
    public ResponseEntity<List<Course>> getPublishedCoursesByType(@PathVariable String type) {
        return ResponseEntity.ok(courseService.getPublishedCoursesByType(type));
    }

    // Get published course by ID (only if published)
    @GetMapping("/published/{id}")
    public ResponseEntity<?> getPublishedCourseById(@PathVariable Long id) {
        try {
            Course course = courseService.getPublishedCourseById(id);
            if (course != null) {
                return ResponseEntity.ok(course);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Course not found or not published");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Search in published courses only
    @GetMapping("/published/search")
    public ResponseEntity<List<Course>> searchPublishedCourses(@RequestParam String keyword) {
        List<Course> courses = courseService.searchPublishedCourses(keyword);
        return ResponseEntity.ok(courses);
    }

    // Get published courses by level
    @GetMapping("/published/level/{level}")
    public ResponseEntity<List<Course>> getPublishedCoursesByLevel(@PathVariable String level) {
        List<Course> courses = courseService.getPublishedCoursesByLevel(level);
        return ResponseEntity.ok(courses);
    }

    // Get published courses by category
    @GetMapping("/published/category/{category}")
    public ResponseEntity<List<Course>> getPublishedCoursesByCategory(@PathVariable String category) {
        List<Course> courses = courseService.getPublishedCoursesByCategory(category);
        return ResponseEntity.ok(courses);
    }
}