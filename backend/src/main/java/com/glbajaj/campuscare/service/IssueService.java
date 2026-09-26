package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.CommonDtos.PageResponse;
import com.glbajaj.campuscare.dto.IssueDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.*;
import com.glbajaj.campuscare.util.EntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * All business rules of an issue's life cycle live here (controllers stay thin).
 *
 *  Student : report -> track -> confirm (CLOSED) or reopen (REOPENED) -> feedback
 *  Admin   : review -> assign / reassign / reject
 *  Staff   : start work (IN_PROGRESS) -> resolve (RESOLVED, with note + optional photo)
 *
 * Every state change goes through {@link IssueWorkflow} and writes a row to issue_status_history (the timeline).
 */
@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final IssueAssignmentRepository assignmentRepository;
    private final IssueStatusHistoryRepository historyRepository;
    private final FeedbackRepository feedbackRepository;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorage;
    private final EntityMapper mapper;

    public IssueService(IssueRepository issueRepository, StudentRepository studentRepository, StaffRepository staffRepository,
                        IssueAssignmentRepository assignmentRepository, IssueStatusHistoryRepository historyRepository,
                        FeedbackRepository feedbackRepository, CategoryService categoryService, LocationService locationService,
                        NotificationService notificationService, FileStorageService fileStorage, EntityMapper mapper) {
        this.issueRepository = issueRepository;
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.feedbackRepository = feedbackRepository;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.notificationService = notificationService;
        this.fileStorage = fileStorage;
        this.mapper = mapper;
    }

    // =====================================================================================================
    // Create
    // =====================================================================================================
    @Transactional
    public IssueDetail create(User user, IssueCreateRequest req, MultipartFile photo) {
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can report issues."));
        Category category = categoryService.find(req.categoryId());
        if (category.getStatus() != RecordStatus.ACTIVE) throw ApiException.badRequest("This category is no longer available.");
        Location location = locationService.find(req.locationId());
        if (location.getStatus() != RecordStatus.ACTIVE) throw ApiException.badRequest("This location is no longer available.");

        Issue issue = new Issue();
        issue.setIssueNumber("TMP-" + UUID.randomUUID());          // replaced below once the database id is known
        issue.setTitle(req.title().trim());
        issue.setDescription(req.description().trim());
        issue.setCategory(category);
        issue.setLocation(location);
        issue.setStudent(student);
        issue.setPriority(req.priority());
        issue.setStatus(IssueStatus.REPORTED);
        issue.setContactNumber(blankToNull(req.contactNumber()));
        issue.setRoomNumber(blankToNull(req.roomNumber()));
        issue.setImageUrl(fileStorage.store(photo, "issues"));
        issue = issueRepository.save(issue);
        issue.setIssueNumber("CC-" + (1000 + issue.getId()));      // CC-1001, CC-1002 ...

        addHistory(issue, null, IssueStatus.REPORTED, user, "Issue reported");
        notificationService.notify(user, issue, "Issue created", "Your issue " + issue.getIssueNumber() + " has been reported.");
        notificationService.notifyAdmins(issue, "New issue reported",
                issue.getIssueNumber() + ": " + issue.getTitle() + " (" + issue.getPriority() + ")");
        if (issue.getPriority() == Priority.CRITICAL) {
            notificationService.notifyAdmins(issue, "Critical issue", issue.getIssueNumber() + " is marked CRITICAL and needs immediate attention.");
        }
        return mapper.detail(issue);
    }

    // =====================================================================================================
    // Read
    // =====================================================================================================
    @Transactional(readOnly = true)
    public IssueDetail get(Long id, User viewer) {
        return mapper.detail(loadAccessible(id, viewer));
    }

    /**
     * Role-scoped, filtered, paged search. STUDENT sees only own issues, STAFF only issues assigned to them,
     * ADMIN sees everything. (The scope is applied here, on the server - the client cannot widen it.)
     */
    @Transactional(readOnly = true)
    public PageResponse<IssueSummary> search(IssueFilter filter, int page, int size, User viewer) {
        IssueFilter scoped = scope(filter, viewer);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Issue> result = issueRepository.findAll(specification(scoped), pageable);
        return new PageResponse<>(result.getContent().stream().map(mapper::summary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Un-paged list for analytics and export (admin only - callers are admin endpoints). */
    @Transactional(readOnly = true)
    public List<Issue> findAll(IssueFilter filter) {
        return issueRepository.findAll(specification(filter), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private org.springframework.data.jpa.domain.Specification<Issue> specification(IssueFilter f) {
        Set<Long> categoryIds = f.categoryId() == null ? null : categoryService.idsWithChildren(f.categoryId());
        Set<Long> locationIds = f.locationId() == null ? null : locationService.idsWithDescendants(f.locationId());
        return IssueSpecifications.from(f, categoryIds, locationIds);
    }

    private IssueFilter scope(IssueFilter filter, User viewer) {
        return switch (viewer.getRole()) {
            case ADMIN -> filter;
            case STUDENT -> filter.withStudent(studentRepository.findByUserId(viewer.getId())
                    .orElseThrow(() -> ApiException.forbidden("Student profile not found.")).getId());
            case STAFF -> filter.withStaff(staffRepository.findByUserId(viewer.getId())
                    .orElseThrow(() -> ApiException.forbidden("Staff profile not found.")).getId());
        };
    }

    /**
     * OWNERSHIP CHECK. A student may open only his own issue, staff only issues currently assigned to them.
     * Changing the id in the URL does not help: this check runs on the server for every request.
     */
    private Issue loadAccessible(Long id, User viewer) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> ApiException.notFound("Issue not found"));
        switch (viewer.getRole()) {
            case ADMIN -> { }
            case STUDENT -> {
                if (!issue.getStudent().getUser().getId().equals(viewer.getId()))
                    throw ApiException.forbidden("You do not have access to this issue.");
            }
            case STAFF -> {
                Staff assigned = issue.getAssignedStaff();
                if (assigned == null || !assigned.getUser().getId().equals(viewer.getId()))
                    throw ApiException.forbidden("This issue is not assigned to you.");
            }
        }
        return issue;
    }

    // =====================================================================================================
    // Edit (student while still REPORTED, admin until final)
    // =====================================================================================================
    @Transactional
    public IssueDetail update(Long id, IssueUpdateRequest req, User actor) {
        Issue issue = loadAccessible(id, actor);
        if (issue.getStatus() == IssueStatus.CLOSED || issue.getStatus() == IssueStatus.REJECTED) {
            throw ApiException.badRequest("A closed or rejected issue cannot be edited.");
        }
        if (actor.getRole() == Role.STUDENT) {
            if (issue.getStatus() != IssueStatus.REPORTED) throw ApiException.badRequest("You can edit an issue only until it has been assigned.");
            if (req.categoryId() != null || req.locationId() != null) throw ApiException.forbidden("Only an administrator can change the category or location.");
        }
        if (req.title() != null && !req.title().isBlank()) issue.setTitle(req.title().trim());
        if (req.description() != null && !req.description().isBlank()) issue.setDescription(req.description().trim());
        if (req.priority() != null) issue.setPriority(req.priority());
        if (req.contactNumber() != null) issue.setContactNumber(blankToNull(req.contactNumber()));
        if (req.roomNumber() != null) issue.setRoomNumber(blankToNull(req.roomNumber()));
        if (req.categoryId() != null) issue.setCategory(categoryService.find(req.categoryId()));
        if (req.locationId() != null) issue.setLocation(locationService.find(req.locationId()));
        addHistory(issue, issue.getStatus(), issue.getStatus(), actor, "Issue details updated by " + actor.getName());
        return mapper.detail(issue);
    }

    // =====================================================================================================
    // Assignment (admin)
    // =====================================================================================================
    /** Assign (or reassign) an issue to an ACTIVE staff member. The staff member's department becomes the issue's department. */
    @Transactional
    public IssueDetail assign(Long id, AssignRequest req, User admin) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> ApiException.notFound("Issue not found"));
        IssueWorkflow.require(issue.getStatus(), IssueStatus.ASSIGNED);

        Staff staff = staffRepository.findById(req.staffId()).orElseThrow(() -> ApiException.notFound("Staff member not found"));
        if (staff.getUser().getStatus() != UserStatus.ACTIVE) {
            throw ApiException.badRequest("This staff member is inactive. Please choose an active staff member.");
        }
        if (staff.getDepartment().getStatus() != RecordStatus.ACTIVE) throw ApiException.badRequest("The staff member's department is inactive.");

        Staff previous = issue.getAssignedStaff();
        IssueStatus old = issue.getStatus();
        boolean sameStaffStillWorking = previous != null && previous.getId().equals(staff.getId())
                && (old == IssueStatus.ASSIGNED || old == IssueStatus.IN_PROGRESS);
        if (sameStaffStillWorking) throw ApiException.badRequest("The issue is already assigned to this staff member.");

        LocalDateTime now = LocalDateTime.now();
        for (IssueAssignment open : assignmentRepository.findByIssueIdAndUnassignedAtIsNull(id)) open.setUnassignedAt(now);
        IssueAssignment assignment = new IssueAssignment();
        assignment.setIssue(issue);
        assignment.setStaff(staff);
        assignment.setAssignedBy(admin);
        assignment.setAssignedAt(now);
        assignmentRepository.save(assignment);

        issue.setAssignedStaff(staff);
        issue.setAssignedDepartment(staff.getDepartment());
        issue.setStatus(IssueStatus.ASSIGNED);
        issue.setResolvedAt(null);

        boolean reassignment = previous != null;
        String deptName = staff.getDepartment().getName();
        String comment = (reassignment ? "Reassigned to " : "Assigned to ") + staff.getUser().getName() + " (" + deptName + ")"
                + (blankToNull(req.comment()) == null ? "" : ": " + req.comment().trim());
        addHistory(issue, old, IssueStatus.ASSIGNED, admin, comment);

        notificationService.notify(staff.getUser(), issue, reassignment ? "Reassignment" : "New assignment",
                issue.getIssueNumber() + ": " + issue.getTitle() + " (" + issue.getPriority() + ") has been assigned to you.");
        if (previous != null && !previous.getId().equals(staff.getId()) && previous.getUser().getStatus() == UserStatus.ACTIVE) {
            notificationService.notify(previous.getUser(), issue, "Issue reassigned",
                    issue.getIssueNumber() + " has been reassigned to another staff member.");
        }
        notificationService.notify(issue.getStudent().getUser(), issue, "Issue assigned",
                "Your issue " + issue.getIssueNumber() + " has been assigned to " + deptName + ".");
        return mapper.detail(issue);
    }

    // =====================================================================================================
    // Status change by staff / admin: IN_PROGRESS, RESOLVED (or REJECTED by admin)
    // =====================================================================================================
    @Transactional
    public IssueDetail changeStatus(Long id, StatusUpdateRequest req, MultipartFile photo, User actor) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> ApiException.notFound("Issue not found"));
        IssueStatus target = req.status();

        // Nobody except the student may close an issue.
        if (target == IssueStatus.CLOSED) throw ApiException.badRequest("Only the student can confirm the resolution and close the issue.");
        if (target != IssueStatus.IN_PROGRESS && target != IssueStatus.RESOLVED && target != IssueStatus.REJECTED) {
            throw ApiException.badRequest("Use the assign or reopen action for this change.");
        }
        if (actor.getRole() == Role.STAFF) {
            Staff assigned = issue.getAssignedStaff();
            if (assigned == null || !assigned.getUser().getId().equals(actor.getId()))
                throw ApiException.forbidden("This issue is not assigned to you.");
            if (target == IssueStatus.REJECTED) throw ApiException.forbidden("Only an administrator can reject an issue.");
        }

        IssueStatus old = issue.getStatus();
        IssueWorkflow.require(old, target);
        String comment = blankToNull(req.comment());

        switch (target) {
            case IN_PROGRESS -> {
                issue.setStatus(IssueStatus.IN_PROGRESS);
                addHistory(issue, old, target, actor, comment == null ? "Work started" : "Work started: " + comment);
                notificationService.notify(issue.getStudent().getUser(), issue, "Work started",
                        "Work has started on your issue " + issue.getIssueNumber() + ".");
            }
            case RESOLVED -> {
                if (comment == null || comment.length() < 5) throw ApiException.badRequest("A resolution note is required (at least 5 characters).");
                issue.setStatus(IssueStatus.RESOLVED);
                issue.setResolutionNote(comment);
                issue.setResolutionImageUrl(fileStorage.store(photo, "resolutions"));
                issue.setResolvedAt(LocalDateTime.now());
                addHistory(issue, old, target, actor, "Resolution note: " + comment);
                notificationService.notify(issue.getStudent().getUser(), issue, "Issue resolved",
                        issue.getIssueNumber() + " was marked resolved. Please confirm the resolution or tell us it is still not fixed.");
            }
            case REJECTED -> {
                if (comment == null) throw ApiException.badRequest("Please give a reason for rejecting the issue.");
                issue.setStatus(IssueStatus.REJECTED);
                addHistory(issue, old, target, actor, "Rejected: " + comment);
                notificationService.notify(issue.getStudent().getUser(), issue, "Issue rejected",
                        issue.getIssueNumber() + " was rejected: " + comment);
            }
            default -> throw ApiException.badRequest("Unsupported status change.");
        }
        return mapper.detail(issue);
    }

    // =====================================================================================================
    // Student confirmation
    // =====================================================================================================
    /** STUDENT CONFIRMATION: the only way an issue becomes CLOSED. RESOLVED -> CLOSED. */
    @Transactional
    public IssueDetail confirm(Long id, User student) {
        Issue issue = loadAccessible(id, student);
        IssueWorkflow.require(issue.getStatus(), IssueStatus.CLOSED);
        IssueStatus old = issue.getStatus();
        issue.setStatus(IssueStatus.CLOSED);
        issue.setClosedAt(LocalDateTime.now());
        addHistory(issue, old, IssueStatus.CLOSED, student, "Student confirmed the resolution");
        notificationService.notify(student, issue, "Issue closed", "Thank you! " + issue.getIssueNumber() + " is now closed.");
        return mapper.detail(issue);
    }

    /** "Issue still not resolved": RESOLVED -> REOPENED. The reason is mandatory. The admin must review and reassign. */
    @Transactional
    public IssueDetail reopen(Long id, ReopenRequest req, User student) {
        Issue issue = loadAccessible(id, student);
        IssueWorkflow.require(issue.getStatus(), IssueStatus.REOPENED);
        IssueStatus old = issue.getStatus();
        issue.setStatus(IssueStatus.REOPENED);
        issue.setResolvedAt(null);
        issue.setResolutionNote(null);
        issue.setResolutionImageUrl(null);
        addHistory(issue, old, IssueStatus.REOPENED, student, "Student reopened the issue: " + req.reason().trim());
        notificationService.notify(student, issue, "Issue reopened", issue.getIssueNumber() + " has been reopened and sent back for admin review.");
        notificationService.notifyAdmins(issue, "Issue reopened", issue.getIssueNumber() + " was reopened by the student: " + req.reason().trim());
        if (issue.getAssignedStaff() != null && issue.getAssignedStaff().getUser().getStatus() == UserStatus.ACTIVE) {
            notificationService.notify(issue.getAssignedStaff().getUser(), issue, "Issue reopened",
                    issue.getIssueNumber() + " was reopened by the student. The admin will review it.");
        }
        return mapper.detail(issue);
    }

    // =====================================================================================================
    // Feedback
    // =====================================================================================================
    @Transactional
    public FeedbackDto feedback(Long id, FeedbackRequest req, User student) {
        Issue issue = loadAccessible(id, student);
        if (issue.getStatus() != IssueStatus.CLOSED) throw ApiException.badRequest("You can rate an issue after you have confirmed the resolution.");
        if (feedbackRepository.existsByIssueId(id)) throw ApiException.conflict("Feedback has already been submitted for this issue.");
        Feedback f = new Feedback();
        f.setIssue(issue);
        f.setStudent(issue.getStudent());
        f.setRating(req.rating());
        f.setComment(blankToNull(req.comment()));
        f = feedbackRepository.save(f);
        return new FeedbackDto(f.getRating(), f.getComment(), f.getCreatedAt());
    }

    // =====================================================================================================
    // helpers
    // =====================================================================================================
    private void addHistory(Issue issue, IssueStatus oldStatus, IssueStatus newStatus, User by, String comment) {
        IssueStatusHistory h = new IssueStatusHistory();
        h.setIssue(issue);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setChangedBy(by);
        h.setComment(comment != null && comment.length() > 1000 ? comment.substring(0, 1000) : comment);
        h.setChangedAt(LocalDateTime.now());
        historyRepository.save(h);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
