package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AnalyticsDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.repository.IssueStatusHistoryRepository;
import com.glbajaj.campuscare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin analytics. Everything is calculated from the real database rows that match the filter -
 * nothing is hard-coded. (For a college-sized data set it is simplest and clearest to aggregate in Java.)
 */
@Service
public class AnalyticsService {
    private final IssueService issueService;
    private final SlaService slaService;
    private final UserRepository userRepository;
    private final IssueStatusHistoryRepository historyRepository;

    public AnalyticsService(IssueService issueService, SlaService slaService, UserRepository userRepository,
                            IssueStatusHistoryRepository historyRepository) {
        this.issueService = issueService;
        this.slaService = slaService;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse compute(IssueFilter filter, String rangeLabel) {
        List<Issue> issues = issueService.findAll(filter);
        long total = issues.size();

        long pending = count(issues, IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.REOPENED);
        long inProgress = count(issues, IssueStatus.IN_PROGRESS);
        long resolved = count(issues, IssueStatus.RESOLVED, IssueStatus.CLOSED);
        long reopened = count(issues, IssueStatus.REOPENED);
        long criticalOpen = issues.stream().filter(i -> i.getPriority() == Priority.CRITICAL
                && SlaService.openStatuses().contains(i.getStatus())).count();
        long activeStaff = userRepository.findByRoleAndStatus(Role.STAFF, UserStatus.ACTIVE).size();
        long inactiveStaff = userRepository.findByRoleAndStatus(Role.STAFF, UserStatus.INACTIVE).size();
        Cards cards = new Cards(total, pending, inProgress, resolved, reopened, criticalOpen, activeStaff, inactiveStaff);

        OptionalDouble avg = issues.stream().filter(i -> i.getResolvedAt() != null)
                .mapToDouble(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes() / 60.0).average();
        Double avgHours = avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : null;
        double rate = total == 0 ? 0 : Math.round((resolved * 1000.0) / total) / 10.0;
        long overdue = issues.stream().filter(slaService::isOverdue).count();
        Set<Long> everReopenedIds = new HashSet<>(historyRepository.findIssueIdsByNewStatus(IssueStatus.REOPENED));
        long everReopened = issues.stream().filter(i -> everReopenedIds.contains(i.getId())).count();
        Metrics metrics = new Metrics(avgHours, rate, overdue, everReopened);

        List<NameCount> byStatus = new ArrayList<>();
        for (IssueStatus s : IssueStatus.values()) byStatus.add(new NameCount(s.name(), count(issues, s)));
        List<NameCount> byPriority = new ArrayList<>();
        for (Priority p : Priority.values()) byPriority.add(new NameCount(p.name(), issues.stream().filter(i -> i.getPriority() == p).count()));

        return new AnalyticsResponse(cards, metrics,
                group(issues, i -> {
                    Category c = i.getCategory();
                    return (c.getParent() != null ? c.getParent() : c).getName();
                }),
                group(issues, i -> {
                    Location l = i.getLocation();
                    while (l.getParent() != null) l = l.getParent();
                    return l.getName();
                }),
                group(issues, i -> i.getAssignedDepartment() == null ? "Unassigned" : i.getAssignedDepartment().getName()),
                byStatus, byPriority, trend(issues, filter), rangeLabel);
    }

    private static long count(List<Issue> issues, IssueStatus... statuses) {
        Set<IssueStatus> set = EnumSet.copyOf(Arrays.asList(statuses));
        return issues.stream().filter(i -> set.contains(i.getStatus())).count();
    }

    private static List<NameCount> group(List<Issue> issues, Function<Issue, String> key) {
        Map<String, Long> counts = issues.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new NameCount(e.getKey(), e.getValue())).toList();
    }

    /** Issues created per day (or per month when the range is longer than ~4 months). */
    private static List<DatePoint> trend(List<Issue> issues, IssueFilter f) {
        LocalDate end = f.to() != null ? f.to().toLocalDate().minusDays(1) : LocalDate.now();
        LocalDate start;
        if (f.from() != null) start = f.from().toLocalDate();
        else if (issues.isEmpty()) start = end.minusDays(29);
        else start = issues.stream().map(i -> i.getCreatedAt().toLocalDate()).min(Comparator.naturalOrder()).orElse(end.minusDays(29));
        if (start.isAfter(end)) start = end;

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        List<DatePoint> points = new ArrayList<>();
        if (days > 120) {
            Map<YearMonth, Long> byMonth = issues.stream().collect(Collectors.groupingBy(i -> YearMonth.from(i.getCreatedAt()), Collectors.counting()));
            for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
                points.add(new DatePoint(m.toString(), byMonth.getOrDefault(m, 0L)));
            }
        } else {
            Map<LocalDate, Long> byDay = issues.stream().collect(Collectors.groupingBy(i -> i.getCreatedAt().toLocalDate(), Collectors.counting()));
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) points.add(new DatePoint(d.toString(), byDay.getOrDefault(d, 0L)));
        }
        return points;
    }
}
