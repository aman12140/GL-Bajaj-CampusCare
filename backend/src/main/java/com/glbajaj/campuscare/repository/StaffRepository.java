package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUserId(Long userId);
    boolean existsByEmployeeIdIgnoreCase(String employeeId);
    Optional<Staff> findByEmployeeIdIgnoreCase(String employeeId);
    long countByDepartmentId(Long departmentId);
}
