package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {
    long countByStudentId(Long studentId);
    long countByStudentIdAndStatusIn(Long studentId, Collection<IssueStatus> statuses);
    long countByAssignedStaffId(Long staffId);
    long countByAssignedStaffIdAndStatusIn(Long staffId, Collection<IssueStatus> statuses);
}
