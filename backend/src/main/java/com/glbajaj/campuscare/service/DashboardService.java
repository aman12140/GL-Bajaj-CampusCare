package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AnalyticsDtos.RoleDashboard;
import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.entity.User;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.IssueRepository;
import com.glbajaj.campuscare.repository.StaffRepository;
import com.glbajaj.campuscare.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Card counts + recent issues for the student and staff dashboards. */
@Service
public class DashboardService {
    private static final Set<IssueStatus> PENDING = Set.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.REOPENED);
    private static final Set<IssueStatus> IN_PROGRESS = Set.of(IssueStatus.IN_PROGRESS);
    private static final Set<IssueStatus> DONE = Set.of(IssueStatus.RESOLVED, IssueStatus.CLOSED);
    private static final Set<IssueStatus> AWAITING = Set.of(IssueStatus.RESOLVED);

    private final IssueRepository issueRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final IssueService issueService;

    public DashboardService(IssueRepository issueRepository, StudentRepository studentRepository, StaffRepository staffRepository,
                            IssueService issueService) {
        this.issueRepository = issueRepository;
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
        this.issueService = issueService;
    }

    @Transactional(readOnly = true)
    public RoleDashboard student(User user) {
        Long sid = studentRepository.findByUserId(user.getId()).orElseThrow(() -> ApiException.forbidden("Student profile not found.")).getId();
        return new RoleDashboard(issueRepository.countByStudentId(sid),
                issueRepository.countByStudentIdAndStatusIn(sid, PENDING),
                issueRepository.countByStudentIdAndStatusIn(sid, IN_PROGRESS),
                issueRepository.countByStudentIdAndStatusIn(sid, DONE),
                issueRepository.countByStudentIdAndStatusIn(sid, AWAITING),
                issueService.search(IssueFilter.none(), 0, 5, user).content());
    }

    @Transactional(readOnly = true)
    public RoleDashboard staff(User user) {
        Long sid = staffRepository.findByUserId(user.getId()).orElseThrow(() -> ApiException.forbidden("Staff profile not found.")).getId();
        return new RoleDashboard(issueRepository.countByAssignedStaffId(sid),
                issueRepository.countByAssignedStaffIdAndStatusIn(sid, Set.of(IssueStatus.ASSIGNED, IssueStatus.REOPENED)),
                issueRepository.countByAssignedStaffIdAndStatusIn(sid, IN_PROGRESS),
                issueRepository.countByAssignedStaffIdAndStatusIn(sid, DONE),
                issueRepository.countByAssignedStaffIdAndStatusIn(sid, AWAITING),
                issueService.search(IssueFilter.none(), 0, 5, user).content());
    }
}
