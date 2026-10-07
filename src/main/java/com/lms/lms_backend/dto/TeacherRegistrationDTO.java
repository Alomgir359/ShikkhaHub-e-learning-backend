package com.lms.lms_backend.dto;

import org.springframework.web.multipart.MultipartFile;

public class TeacherRegistrationDTO {
    // Step 1
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String currentProfession;
    private String organization;
    private String experience;

    // Step 2 - Files
    private MultipartFile cv;
    private MultipartFile nidPhoto;
    private MultipartFile profilePhoto;
    private MultipartFile organizationIdCard;

    // Getters and Setters
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

    public MultipartFile getCv() { return cv; }
    public void setCv(MultipartFile cv) { this.cv = cv; }

    public MultipartFile getNidPhoto() { return nidPhoto; }
    public void setNidPhoto(MultipartFile nidPhoto) { this.nidPhoto = nidPhoto; }

    public MultipartFile getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(MultipartFile profilePhoto) { this.profilePhoto = profilePhoto; }

    public MultipartFile getOrganizationIdCard() { return organizationIdCard; }
    public void setOrganizationIdCard(MultipartFile organizationIdCard) { this.organizationIdCard = organizationIdCard; }
}