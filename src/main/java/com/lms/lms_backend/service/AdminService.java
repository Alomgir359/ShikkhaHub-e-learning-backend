package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Admin;
import com.lms.lms_backend.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public Admin login(String username, String password) {
        Optional<Admin> admin = adminRepository.findByUsernameAndPassword(username, password);
        if (admin.isPresent()) {
            Admin a = admin.get();
            a.setLastLogin(LocalDateTime.now());
            adminRepository.save(a);
            return a;
        }
        return null;
    }

    public Admin findByUsername(String username) {
        return adminRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public Admin createDefaultAdmin() {
        if (!adminRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setFullName("System Administrator");
            admin.setEmail("admin@lms.com");
            return adminRepository.save(admin);
        }
        return null;
    }
}