package com.glbajaj.campuscare.dto;

import com.glbajaj.campuscare.dto.IssueDtos.IssueSummary;

import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record NameCount(String name, long count) {}

    public record DatePoint(String date, long count) {}

    public record Cards(long total, long pending, long inProgress, long resolved, long reopened, long critical,
                        long activeStaff, long inactiveStaff) {}

    public record Metrics(Double averageResolutionHours, double resolutionRatePercent, long overdueIssues,
                          long everReopenedIssues) {}

    public record AnalyticsResponse(Cards cards, Metrics metrics,
                                    List<NameCount> byCategory, List<NameCount> byLocation, List<NameCount> byDepartment,
                                    List<NameCount> byStatus, List<NameCount> byPriority, List<DatePoint> trend,
                                    String rangeLabel) {}

    /** Student / staff dashboard payload. */
    public record RoleDashboard(long total, long pending, long inProgress, long resolved, long awaitingConfirmation,
                                List<IssueSummary> recent) {}

    public record NotificationDto(Long id, Long issueId, String issueNumber, String title, String message,
                                  boolean read, java.time.LocalDateTime createdAt) {}
}
