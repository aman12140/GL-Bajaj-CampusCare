package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByParentId(Long parentId);
}
