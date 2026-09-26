package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    boolean existsByStudentIdIgnoreCase(String studentId);
}
