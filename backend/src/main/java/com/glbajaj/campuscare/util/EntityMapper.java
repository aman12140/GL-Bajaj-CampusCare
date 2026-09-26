package com.glbajaj.campuscare.util;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.dto.AuthDtos.ProfileDto;
import com.glbajaj.campuscare.dto.IssueDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.repository.*;
import com.glbajaj.campuscare.service.SlaService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts entities to DTOs. DTOs are what the API returns, so sensitive fields (password hash) can never leak.
 * Must be called inside a transaction (lazy relations are read here).
 */
@Component
public class EntityMapper {
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final IssueRepository issueRepository;
    private final IssueStatusHistoryRepository historyRepository;
    private final FeedbackRepository feedbackRepository;
    private final SlaService slaService;

    public EntityMapper(StudentRepository studentRepository, StaffRepository staffRepository, IssueRepository issueRepository,
                        IssueStatusHistoryRepository historyRepository, FeedbackRepository feedbackRepository, SlaService slaService) {
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
        this.issueRepository = issueRepository;
        this.historyRepository = historyRepository;
        this.feedbackRepository = feedbackRepository;
        this.slaService = slaService;
    }

    // ---------- paths ----------
    /** "Block A > 2nd Floor > Demo Lab A" */
    public static String locationPath(Location loc) {
        List<String> parts = new ArrayList<>();
        for (Location l = loc; l != null; l = l.getParent()) parts.add(l.getName());
        Collections.reverse(parts);
        return String.join(" > ", parts);
    }

    /** Location path of an issue, with the student's room number appended: "Block A > 2nd Floor > Room 204". */
    public static String issueLocation(Issue i) {
        String path = locationPath(i.getLocation());
        String room = i.getRoomNumber();
        return room == null || room.isBlank() ? path : path + " > Room " + room.trim();
    }

    /** "IT & Internet > Wi-Fi / Internet" */
    public static String categoryPath(Category c) {
        return c.getParent() == null ? c.getName() : c.getParent().getName() + " > " + c.getName();
    }

    /** Suggested department: the category's own, otherwise its parent's. */
    public static Department suggestedDepartment(Category c) {
        if (c.getDepartment() != null) return c.getDepartment();
        return c.getParent() != null ? c.getParent().getDepartment() : null;
    }

    // ---------- profile ----------
    public ProfileDto profile(User u) {
        Student st = u.getRole() == Role.STUDENT ? studentRepository.findByUserId(u.getId()).orElse(null) : null;
        Staff sf = u.getRole() == Role.STAFF ? staffRepository.findByUserId(u.getId()).orElse(null) : null;
        return new ProfileDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getStatus(),
                u.isPasswordChangeRequired(),
                st == null ? null : st.getStudentId(), st == null ? null : st.getCourse(), st == null ? null : st.getBranch(),
                st == null ? null : st.getYear(), st == null ? null : st.getSection(),
                sf == null ? null : sf.getId(), sf == null ? null : sf.getEmployeeId(),
                sf == null ? null : sf.getDepartment().getId(), sf == null ? null : sf.getDepartment().getName(),
                sf == null ? null : sf.getDesignation());
    }

    // ---------- issues ----------
    public IssueSummary summary(Issue i) {
        return new IssueSummary(i.getId(), i.getIssueNumber(), i.getTitle(), categoryPath(i.getCategory()),
                issueLocation(i), i.getPriority(), i.getStatus(), i.getCreatedAt(), i.getUpdatedAt(),
                i.getResolvedAt(), i.getStudent().getUser().getName(),
                i.getAssignedStaff() == null ? null : i.getAssignedStaff().getUser().getName(),
                i.getAssignedDepartment() == null ? null : i.getAssignedDepartment().getName(),
                slaService.isOverdue(i));
    }

    public IssueDetail detail(Issue i) {
        Student st = i.getStudent();
        Staff sf = i.getAssignedStaff();
        Department suggested = suggestedDepartment(i.getCategory());
        List<TimelineEntry> timeline = historyRepository.findByIssueIdOrderByChangedAtAscIdAsc(i.getId()).stream()
                .map(h -> new TimelineEntry(h.getOldStatus() == null ? null : h.getOldStatus().name(), h.getNewStatus().name(),
                        h.getChangedBy().getName(), h.getChangedBy().getRole().name(), h.getComment(), h.getChangedAt()))
                .toList();
        FeedbackDto feedback = feedbackRepository.findByIssueId(i.getId())
                .map(f -> new FeedbackDto(f.getRating(), f.getComment(), f.getCreatedAt())).orElse(null);
        StaffRef staffRef = sf == null ? null : new StaffRef(sf.getId(), sf.getUser().getName(), sf.getEmployeeId(),
                sf.getDesignation(), sf.getUser().getStatus().name());
        return new IssueDetail(i.getId(), i.getIssueNumber(), i.getTitle(), i.getDescription(),
                i.getCategory().getId(), categoryPath(i.getCategory()), i.getLocation().getId(), issueLocation(i), i.getRoomNumber(),
                i.getPriority(), i.getStatus(), i.getImageUrl(), i.getContactNumber(),
                st.getUser().getName(), st.getStudentId(), st.getUser().getEmail(), st.getUser().getPhone(),
                staffRef,
                i.getAssignedDepartment() == null ? null : i.getAssignedDepartment().getId(),
                i.getAssignedDepartment() == null ? null : i.getAssignedDepartment().getName(),
                suggested == null ? null : suggested.getId(), suggested == null ? null : suggested.getName(),
                i.getResolutionNote(), i.getResolutionImageUrl(),
                i.getCreatedAt(), i.getUpdatedAt(), i.getResolvedAt(), i.getClosedAt(),
                slaService.hoursFor(i.getPriority()), slaService.dueAt(i), slaService.isOverdue(i), feedback, timeline);
    }

    // ---------- admin master data ----------
    public StaffDto staff(Staff s) {
        return new StaffDto(s.getId(), s.getUser().getId(), s.getUser().getName(), s.getEmployeeId(), s.getUser().getEmail(),
                s.getUser().getPhone(), s.getDepartment().getId(), s.getDepartment().getName(), s.getDesignation(),
                s.getUser().getStatus(),
                issueRepository.countByAssignedStaffIdAndStatusIn(s.getId(), SlaService.openStatuses()),
                issueRepository.countByAssignedStaffId(s.getId()));
    }

    public DepartmentDto department(Department d) {
        return new DepartmentDto(d.getId(), d.getName(), d.getDescription(), d.getStatus(), staffRepository.countByDepartmentId(d.getId()));
    }

    public CategoryDto category(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(),
                c.getParent() == null ? null : c.getParent().getId(), c.getParent() == null ? null : c.getParent().getName(),
                c.getDepartment() == null ? null : c.getDepartment().getId(),
                c.getDepartment() == null ? null : c.getDepartment().getName(), c.getStatus());
    }

    public LocationDto location(Location l) {
        return new LocationDto(l.getId(), l.getName(), l.getType(), l.getLevel(),
                l.getParent() == null ? null : l.getParent().getId(), l.getFloor(), l.getStatus(), locationPath(l));
    }
}
