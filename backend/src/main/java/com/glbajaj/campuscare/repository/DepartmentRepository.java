package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Department> findByNameIgnoreCase(String name);
}
