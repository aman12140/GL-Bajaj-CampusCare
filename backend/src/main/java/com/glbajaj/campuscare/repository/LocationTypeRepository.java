package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface LocationTypeRepository extends JpaRepository<LocationType, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<LocationType> findByNameIgnoreCase(String name);
}
