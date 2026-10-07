package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    List<Teacher> findByStatus(String status);

    List<Teacher> findByRole(String role);  // ← এই লাইন যোগ করুন

    Optional<Teacher> findByEmailAndPasswordAndStatus(String email, String password, String status);

    boolean existsByEmail(String email);

    Optional<Teacher> findByEmail(String email);

    List<Teacher> findByFullNameContainingIgnoreCase(String fullName);

    long countByStatus(String status);

    List<Teacher> findAllByOrderByIdDesc();

    List<Teacher> findByStatusAndRole(String status, String role);

    long countByStatusAndRole(String status, String role);
}