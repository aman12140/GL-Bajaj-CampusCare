package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId);
}
