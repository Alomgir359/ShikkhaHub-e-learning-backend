package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    Optional<Admin> findByUsernameAndPassword(String username, String password);
    boolean existsByUsername(String username);
}