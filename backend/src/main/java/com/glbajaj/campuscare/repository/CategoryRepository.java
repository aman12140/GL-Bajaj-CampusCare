package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentId(Long parentId);
}
