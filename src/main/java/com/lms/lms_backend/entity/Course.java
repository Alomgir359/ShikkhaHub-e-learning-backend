package com.lms.lms_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseTitle;

    private String subTitle;

    private String instructorName;

    private String instructorExperience;

    private Integer durationInWeeks;

    private Double price;

    private Integer totalSeats;

    private Integer enrolledStudents = 0;

    @Column(length = 2000)
    private String description;

    private String level;

    private String category;

    private String status = "DRAFT";

    private Long teacherId;

    private String teacherEmail;

    @Column(columnDefinition = "TEXT")
    private String weeklyModules; // Store as JSON string

    @Column(columnDefinition = "TEXT")
    private String requirements;
    @Column(nullable = false)
    private Integer isApproved = 0; // 0 = pending, 1 = approved

    @Column(nullable = false)
    private Integer isPublished = 2; // 0 = draft, 1 = published
    @Column(columnDefinition = "TEXT")
    private String learningOutcomes;

    // New fields for CourseDetails page
    @Column(columnDefinition = "TEXT")
    private String audience; // Who this course is for

    @Column(columnDefinition = "TEXT")
    private String whatsIncluded; // What's included in the course

    private String schedule; // Class schedule (e.g., "Sun & Tue - 8 PM")

    private String language; // Course language (e.g., "Bengali", "English")

    private String thumbnailUrl; // Course thumbnail image URL

    private String promoVideoUrl; // Promotional video URL

    private Integer totalClasses; // Total number of classes

    private Integer totalStudents = 0; // Total enrolled students count

    @Column(columnDefinition = "TEXT")
    private String instructorBio; // Detailed instructor biography

    private String instructorAvatar; // Instructor profile picture URL

    private String instructorTitle; // Instructor job title (e.g., "Senior Banking Trainer")

    private Double rating = 0.0; // Course rating (0-5)

    private Integer totalRatings = 0; // Total number of ratings

    // ===== E-learning (batch / live / recorded) fields =====
    private String courseType = "LIVE";        // LIVE, OFFLINE or RECORDED

    private Integer batchNumber;               // e.g. 13

    private LocalDate batchStartDate;          // first class date

    private String classDays;                  // e.g. "Sat, Wed"

    private String classTime;                  // e.g. "9:00 PM - 10:30 PM"

    private String supportClassSchedule;       // e.g. "Sun, Tue, Thu - 10:30 PM - 11:30 PM"

    private Double originalPrice;              // price before discount (shown struck-through)

    private LocalDateTime offerEndsAt;         // discount deadline

    @Column(length = 500)
    private String venue;                      // OFFLINE batch: class location / address

    @Column(columnDefinition = "TEXT")
    private String motivationalText;           // Bangla motivational text under the hero facts (edit mode only)

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(unique = true)
    private String enrollmentKey;

    public Course() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "DRAFT";
        this.enrolledStudents = 0;
        this.totalStudents = 0;
        this.rating = 0.0;
        this.totalRatings = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    // Getters and Setters
    public Integer getIsApproved() { return isApproved; }
    public void setIsApproved(Integer isApproved) { this.isApproved = isApproved; }

    public Integer getIsPublished() { return isPublished; }
    public void setIsPublished(Integer isPublished) { this.isPublished = isPublished; }
    public String getSubTitle() { return subTitle; }
    public void setSubTitle(String subTitle) { this.subTitle = subTitle; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getInstructorExperience() { return instructorExperience; }
    public void setInstructorExperience(String instructorExperience) { this.instructorExperience = instructorExperience; }

    public Integer getDurationInWeeks() { return durationInWeeks; }
    public void setDurationInWeeks(Integer durationInWeeks) { this.durationInWeeks = durationInWeeks; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Integer getEnrolledStudents() { return enrolledStudents; }
    public void setEnrolledStudents(Integer enrolledStudents) { this.enrolledStudents = enrolledStudents; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getTeacherEmail() { return teacherEmail; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }

    public String getWeeklyModules() { return weeklyModules; }
    public void setWeeklyModules(String weeklyModules) { this.weeklyModules = weeklyModules; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getLearningOutcomes() { return learningOutcomes; }
    public void setLearningOutcomes(String learningOutcomes) { this.learningOutcomes = learningOutcomes; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getWhatsIncluded() { return whatsIncluded; }
    public void setWhatsIncluded(String whatsIncluded) { this.whatsIncluded = whatsIncluded; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getPromoVideoUrl() { return promoVideoUrl; }
    public void setPromoVideoUrl(String promoVideoUrl) { this.promoVideoUrl = promoVideoUrl; }

    public Integer getTotalClasses() { return totalClasses; }
    public void setTotalClasses(Integer totalClasses) { this.totalClasses = totalClasses; }

    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }

    public String getInstructorBio() { return instructorBio; }
    public void setInstructorBio(String instructorBio) { this.instructorBio = instructorBio; }

    public String getInstructorAvatar() { return instructorAvatar; }
    public void setInstructorAvatar(String instructorAvatar) { this.instructorAvatar = instructorAvatar; }

    public String getInstructorTitle() { return instructorTitle; }
    public void setInstructorTitle(String instructorTitle) { this.instructorTitle = instructorTitle; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getEnrollmentKey() { return enrollmentKey; }
    public void setEnrollmentKey(String enrollmentKey) { this.enrollmentKey = enrollmentKey; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public Integer getBatchNumber() { return batchNumber; }
    public void setBatchNumber(Integer batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getBatchStartDate() { return batchStartDate; }
    public void setBatchStartDate(LocalDate batchStartDate) { this.batchStartDate = batchStartDate; }

    public String getClassDays() { return classDays; }
    public void setClassDays(String classDays) { this.classDays = classDays; }

    public String getClassTime() { return classTime; }
    public void setClassTime(String classTime) { this.classTime = classTime; }

    public String getSupportClassSchedule() { return supportClassSchedule; }
    public void setSupportClassSchedule(String supportClassSchedule) { this.supportClassSchedule = supportClassSchedule; }

    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }

    public LocalDateTime getOfferEndsAt() { return offerEndsAt; }
    public void setOfferEndsAt(LocalDateTime offerEndsAt) { this.offerEndsAt = offerEndsAt; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getMotivationalText() { return motivationalText; }
    public void setMotivationalText(String motivationalText) { this.motivationalText = motivationalText; }
}
