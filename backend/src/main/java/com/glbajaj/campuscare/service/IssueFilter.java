package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.entity.Priority;

import java.time.LocalDateTime;
import java.util.Set;

/** All optional filters used by issue lists, analytics and exports. "to" is exclusive. studentId/staffId scope by role. */
public record IssueFilter(String q, Set<IssueStatus> statuses, Priority priority, Long categoryId, Long departmentId,
                          Long locationId, Long staffId, LocalDateTime from, LocalDateTime to, Long studentId) {

    public IssueFilter withStudent(Long id) {
        return new IssueFilter(q, statuses, priority, categoryId, departmentId, locationId, staffId, from, to, id);
    }

    public IssueFilter withStaff(Long id) {
        return new IssueFilter(q, statuses, priority, categoryId, departmentId, locationId, id, from, to, studentId);
    }

    public static IssueFilter none() {
        return new IssueFilter(null, null, null, null, null, null, null, null, null, null);
    }
}
