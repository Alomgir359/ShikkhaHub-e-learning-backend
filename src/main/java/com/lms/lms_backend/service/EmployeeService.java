package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Employee;
import com.lms.lms_backend.entity.Rank;
import com.lms.lms_backend.repository.EmployeeRepository;
import com.lms.lms_backend.repository.RankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final RankRepository rankRepository;

    public EmployeeService(EmployeeRepository employeeRepository, RankRepository rankRepository) {
        this.employeeRepository = employeeRepository;
        this.rankRepository = rankRepository;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id).orElse(null);
    }

    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email).orElse(null);
    }

    public Employee login(String email, String password) {
        return employeeRepository.findByEmailAndPassword(email, password).orElse(null);
    }

    @Transactional
    public Employee createEmployee(Employee employee, Long rankId) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        if (employeeRepository.existsByEmployeeId(employee.getEmployeeId())) {
            throw new RuntimeException("Employee ID already exists!");
        }

        Rank rank = rankRepository.findById(rankId)
                .orElseThrow(() -> new RuntimeException("Rank not found!"));

        employee.setRank(rank);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setPassword(employee.getPassword() != null ? employee.getPassword() : "password123");

        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee employeeDetails, Long rankId) {
        Employee employee = getEmployeeById(id);
        if (employee == null) {
            throw new RuntimeException("Employee not found!");
        }

        if (rankId != null) {
            Rank rank = rankRepository.findById(rankId)
                    .orElseThrow(() -> new RuntimeException("Rank not found!"));
            employee.setRank(rank);
        }

        employee.setFullName(employeeDetails.getFullName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPhone(employeeDetails.getPhone());
        employee.setDepartment(employeeDetails.getDepartment());
        employee.setDesignation(employeeDetails.getDesignation());
        employee.setUpdatedAt(LocalDateTime.now());

        return employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }

    public List<Employee> getEmployeesByRank(Long rankId) {
        Rank rank = rankRepository.findById(rankId).orElse(null);
        if (rank != null) {
            return employeeRepository.findByRank(rank);
        }
        return List.of();
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        Employee employee = getEmployeeById(id);
        if (employee != null) {
            employee.setPassword(newPassword);
            employee.setUpdatedAt(LocalDateTime.now());
            employeeRepository.save(employee);
        }
    }
}