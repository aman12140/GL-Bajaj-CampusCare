package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface IssueAssignmentRepository extends JpaRepository<IssueAssignment, Long> {
    List<IssueAssignment> findByIssueIdAndUnassignedAtIsNull(Long issueId);
}
