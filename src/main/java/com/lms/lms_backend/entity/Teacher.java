package com.lms.lms_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String password;

    // Step 1 fields
    private String currentProfession;
    private String organization;
    @Column(length = 1000)
    private String experience;

    // Step 2 fields - File paths
    private String cvPath;
    private String nidPhotoPath;
    private String profilePhotoPath;
    private String organizationIdPath;

    private String role; // TEACHER, STUDENT, ADMIN
    private String status; // PENDING, APPROVED, REJECTED

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Teacher() {
        this.role = "TEACHER";
        this.status = "PENDING";
        this.appliedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();  // ← এই লাইন যোগ করুন
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCurrentProfession() { return currentProfession; }
    public void setCurrentProfession(String currentProfession) { this.currentProfession = currentProfession; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public String getNidPhotoPath() { return nidPhotoPath; }
    public void setNidPhotoPath(String nidPhotoPath) { this.nidPhotoPath = nidPhotoPath; }

    public String getProfilePhotoPath() { return profilePhotoPath; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }

    public String getOrganizationIdPath() { return organizationIdPath; }
    public void setOrganizationIdPath(String organizationIdPath) { this.organizationIdPath = organizationIdPath; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}