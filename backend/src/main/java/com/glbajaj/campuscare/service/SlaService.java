package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.entity.Issue;
import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.entity.Priority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Demo SLA targets (configurable in application.yml). These are PROJECT/DEMO values - NOT official GL Bajaj policy.
 * An issue is "overdue" when it is still open (not resolved/closed/rejected) and its target time has passed.
 * The clock always starts at the time the issue was reported.
 */
@Service
public class SlaService {
    private static final Set<IssueStatus> OPEN = EnumSet.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED,
            IssueStatus.IN_PROGRESS, IssueStatus.REOPENED);

    private final int low, medium, high, critical;

    public SlaService(@Value("${app.sla.low-hours:72}") int low, @Value("${app.sla.medium-hours:48}") int medium,
                      @Value("${app.sla.high-hours:24}") int high, @Value("${app.sla.critical-hours:6}") int critical) {
        this.low = low; this.medium = medium; this.high = high; this.critical = critical;
    }

    public int hoursFor(Priority p) {
        return switch (p) { case LOW -> low; case MEDIUM -> medium; case HIGH -> high; case CRITICAL -> critical; };
    }

    public LocalDateTime dueAt(Issue issue) { return issue.getCreatedAt().plusHours(hoursFor(issue.getPriority())); }

    public boolean isOverdue(Issue issue) {
        return OPEN.contains(issue.getStatus()) && LocalDateTime.now().isAfter(dueAt(issue));
    }

    public static Set<IssueStatus> openStatuses() { return OPEN; }
}
