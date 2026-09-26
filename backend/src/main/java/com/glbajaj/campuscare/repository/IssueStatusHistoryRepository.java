package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Long> {
    List<IssueStatusHistory> findByIssueIdOrderByChangedAtAscIdAsc(Long issueId);

    @Query("select distinct h.issue.id from IssueStatusHistory h where h.newStatus = :status")
    List<Long> findIssueIdsByNewStatus(@Param("status") IssueStatus status);
}
