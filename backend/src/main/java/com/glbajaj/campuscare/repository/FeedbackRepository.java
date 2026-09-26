package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByIssueId(Long issueId);
    Optional<Feedback> findByIssueId(Long issueId);
}
