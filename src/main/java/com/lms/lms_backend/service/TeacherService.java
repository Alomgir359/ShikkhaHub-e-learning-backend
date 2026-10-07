package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TeacherService {

    private final TeacherRepository repo;

    @Value("${file.upload.directory:uploads/teachers}")
    private String uploadDirectory;

    public TeacherService(TeacherRepository repo) {
        this.repo = repo;
    }

    // Step 1: Save basic info
    @Transactional
    public Long saveTeacherStep1(Teacher teacher) throws Exception {
        // Check if email already exists
        if (repo.existsByEmail(teacher.getEmail())) {
            throw new Exception("Email already exists!");
        }

        // Set default values
        teacher.setRole("TEACHER");
        teacher.setStatus("PENDING");
        teacher.setAppliedAt(LocalDateTime.now());
        teacher.setUpdatedAt(LocalDateTime.now());

        // Save to database
        Teacher saved = repo.save(teacher);
        return saved.getId();
    }

    // Step 2: Upload files and update teacher
    @Transactional
    public String saveTeacherStep2(Long tempId, MultipartFile cv, MultipartFile nidPhoto,
                                   MultipartFile profilePhoto, MultipartFile organizationIdCard) throws IOException {

        Teacher teacher = repo.findById(tempId).orElse(null);
        if (teacher == null) {
            return "Teacher not found!";
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        }

        // Save CV
        if (cv != null && !cv.isEmpty()) {
            String cvFilename = saveFile(cv, "cv_" + tempId);
            teacher.setCvPath(cvFilename);
            System.out.println("CV saved: " + cvFilename);
        }

        // Save NID Photo
        if (nidPhoto != null && !nidPhoto.isEmpty()) {
            String nidFilename = saveFile(nidPhoto, "nid_" + tempId);
            teacher.setNidPhotoPath(nidFilename);
            System.out.println("NID saved: " + nidFilename);
        }

        // Save Profile Photo
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String profileFilename = saveFile(profilePhoto, "profile_" + tempId);
            teacher.setProfilePhotoPath(profileFilename);
            System.out.println("Profile photo saved: " + profileFilename);
        }

        // Save Organization ID Card
        if (organizationIdCard != null && !organizationIdCard.isEmpty()) {
            String orgIdFilename = saveFile(organizationIdCard, "orgid_" + tempId);
            teacher.setOrganizationIdPath(orgIdFilename);
            System.out.println("Organization ID saved: " + orgIdFilename);
        }

        teacher.setUpdatedAt(LocalDateTime.now());
        repo.save(teacher);

        return "Application submitted successfully!";
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        // Get original filename and extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate unique filename
        String filename = prefix + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadDirectory, filename);

        // Save file
        Files.copy(file.getInputStream(), filePath);
        System.out.println("File saved at: " + filePath.toAbsolutePath());

        return filename;
    }

    // Only instructor applications. Students waiting for payment verification are handled
    // on the Enrollment Requests page, so they must not show up here.
    public List<Teacher> getPendingTeachers() {
        return repo.findByStatusAndRole("PENDING", "TEACHER");
    }

    public List<Teacher> getAllTeachers() {
        return repo.findAll();
    }

    public Teacher getTeacherById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Transactional
    public String approveTeacher(Long id) {
        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher != null) {
            teacher.setStatus("APPROVED");
            teacher.setUpdatedAt(LocalDateTime.now());
            repo.save(teacher);
            return "Teacher approved successfully!";
        }
        return "Teacher not found!";
    }

    @Transactional
    public String rejectTeacher(Long id) {
        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher != null) {
            teacher.setStatus("REJECTED");
            teacher.setUpdatedAt(LocalDateTime.now());
            repo.save(teacher);
            return "Teacher rejected!";
        }
        return "Teacher not found!";
    }

    public String deleteTeacher(Long id) {
        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher != null) {
            repo.delete(teacher);
            return "Teacher deleted successfully!";
        }
        return "Teacher not found!";
    }

    // Student self-registration: no admin approval, no documents needed
    @Transactional
    public Teacher registerStudent(String fullName, String email, String phone, String password) throws Exception {
        if (email == null || email.isBlank()) throw new Exception("Email is required");
        String normalizedEmail = email.trim();
        if (repo.existsByEmail(normalizedEmail)) {
            throw new Exception("An account with this email already exists. Please log in.");
        }
        Teacher student = new Teacher();
        student.setFullName(fullName.trim());
        student.setEmail(normalizedEmail);
        student.setPhone(phone == null ? null : phone.trim());
        student.setPassword(password);
        student.setCurrentProfession("Student");
        student.setOrganization("Self");
        student.setExperience("New Student");
        student.setRole("STUDENT");
        student.setStatus("APPROVED");
        student.setAppliedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        return repo.save(student);
    }

    public Teacher login(String email, String password) {
        return repo.findByEmailAndPasswordAndStatus(email, password, "APPROVED").orElse(null);
    }

    public Teacher findByEmail(String email) {
        return repo.findByEmail(email).orElse(null);
    }

    // Login lookup that does NOT filter by status, so the controller can explain
    // "payment under review" / "rejected" instead of a generic error.
    public Teacher findByEmailAndPassword(String email, String password) {
        if (email == null || password == null) return null;
        Teacher t = repo.findByEmail(email.trim()).orElse(null);
        if (t == null || t.getPassword() == null || !t.getPassword().equals(password)) return null;
        return t;
    }

    @Transactional
    public Teacher save(Teacher teacher) {
        teacher.setUpdatedAt(LocalDateTime.now());
        return repo.save(teacher);
    }

    public long getTotalTeacherCount() {
        return repo.count();
    }

    public long getPendingTeacherCount() {
        return repo.countByStatusAndRole("PENDING", "TEACHER");
    }

    public long getApprovedTeacherCount() {
        return repo.countByStatus("APPROVED");
    }

    public long getRejectedTeacherCount() {
        return repo.countByStatus("REJECTED");
    }

    // ========== Update Teacher Profile ==========
    @Transactional
    public Teacher updateTeacher(Teacher teacher) {
        teacher.setUpdatedAt(LocalDateTime.now());
        return repo.save(teacher);
    }

    // ========== নতুন মেথড: Save Profile Picture ==========
    @Transactional
    public String saveProfilePicture(Long id, MultipartFile file) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        }

        // Get teacher
        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher == null) {
            throw new IOException("Teacher not found");
        }

        // Get original filename and extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate unique filename
        String filename = "profile_" + id + "_" + System.currentTimeMillis() + extension;
        Path filePath = Paths.get(uploadDirectory, filename);

        // Save file
        Files.copy(file.getInputStream(), filePath);
        System.out.println("Profile picture saved at: " + filePath.toAbsolutePath());

        // Delete old profile picture if exists
        if (teacher.getProfilePhotoPath() != null && !teacher.getProfilePhotoPath().isEmpty()) {
            Path oldPath = Paths.get(uploadDirectory, teacher.getProfilePhotoPath());
            if (Files.exists(oldPath)) {
                Files.delete(oldPath);
                System.out.println("Old profile picture deleted: " + oldPath.toAbsolutePath());
            }
        }

        return filename;
    }

    // ========== নতুন মেথড: Get Profile Picture as Bytes ==========
    public byte[] getProfilePicture(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDirectory, fileName);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        throw new IOException("File not found: " + fileName);
    }

    // ========== Update Teacher Profile Fields ==========
    @Transactional
    public Teacher updateTeacherProfile(Long id, String fullName, String email, String phone,
                                        String currentProfession, String organization) {
        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher != null) {
            if (fullName != null) teacher.setFullName(fullName);
            if (email != null) teacher.setEmail(email);
            if (phone != null) teacher.setPhone(phone);
            if (currentProfession != null) teacher.setCurrentProfession(currentProfession);
            if (organization != null) teacher.setOrganization(organization);
            teacher.setUpdatedAt(LocalDateTime.now());
            return repo.save(teacher);
        }
        return null;
    }

    // Get all students (role = STUDENT)
    public List<Teacher> getAllStudents() {
        return repo.findByRole("STUDENT");
    }

    // Get student by ID
    public Teacher getStudentById(Long id) {
        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher != null && "STUDENT".equals(teacher.getRole())) {
            return teacher;
        }
        return null;
    }

    // Get all approved teachers
    public List<Teacher> getApprovedTeachers() {
        return repo.findByStatus("APPROVED");
    }

    // Get teachers by role
    public List<Teacher> getTeachersByRole(String role) {
        return repo.findByRole(role);
    }
}