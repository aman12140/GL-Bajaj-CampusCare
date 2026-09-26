package com.glbajaj.campuscare.dto;

import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.entity.Priority;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.service.IssueFilter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Query-string filters shared by issue lists, analytics and exports.
 * range = TODAY | WEEK | MONTH | QUARTER (last 3 months) | CUSTOM (with fromDate / toDate as yyyy-MM-dd).
 * group = PENDING | IN_PROGRESS | DONE (convenience status groups used by the dashboards).
 */
public record IssueFilterParams(String q, IssueStatus status, String group, Priority priority, Long categoryId,
                                Long departmentId, Long locationId, Long staffId, String range, String fromDate, String toDate) {

    public IssueFilter toFilter() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = null, to = null;
        String r = range == null ? "" : range.trim().toUpperCase();
        try {
            switch (r) {
                case "TODAY" -> from = today.atStartOfDay();
                case "WEEK" -> from = today.with(DayOfWeek.MONDAY).atStartOfDay();
                case "MONTH" -> from = today.withDayOfMonth(1).atStartOfDay();
                case "QUARTER" -> from = today.minusMonths(3).atStartOfDay();
                case "CUSTOM" -> {
                    if (fromDate != null && !fromDate.isBlank()) from = LocalDate.parse(fromDate).atStartOfDay();
                    if (toDate != null && !toDate.isBlank()) to = LocalDate.parse(toDate).plusDays(1).atStartOfDay();
                }
                default -> { }
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("Invalid date. Use the format yyyy-MM-dd.");
        }
        return new IssueFilter(q, statuses(), priority, categoryId, departmentId, locationId, staffId, from, to, null);
    }

    private Set<IssueStatus> statuses() {
        if (status != null) return Set.of(status);
        if (group == null) return null;
        return switch (group.trim().toUpperCase()) {
            case "PENDING" -> Set.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.REOPENED);
            case "IN_PROGRESS" -> Set.of(IssueStatus.IN_PROGRESS);
            case "DONE" -> Set.of(IssueStatus.RESOLVED, IssueStatus.CLOSED);
            default -> null;
        };
    }

    /** Human readable label for reports. */
    public String rangeLabel() {
        String r = range == null ? "" : range.trim().toUpperCase();
        return switch (r) {
            case "TODAY" -> "Today";
            case "WEEK" -> "This week";
            case "MONTH" -> "This month";
            case "QUARTER" -> "Last 3 months";
            case "CUSTOM" -> "Custom: " + (fromDate == null ? "start" : fromDate) + " to " + (toDate == null ? "today" : toDate);
            default -> "All time";
        };
    }
}
