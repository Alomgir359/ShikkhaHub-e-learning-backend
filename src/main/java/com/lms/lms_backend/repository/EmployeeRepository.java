package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Employee;
import com.lms.lms_backend.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmailAndPassword(String email, String password);
    List<Employee> findByRank(Rank rank);
    List<Employee> findByStatus(String status);
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
}