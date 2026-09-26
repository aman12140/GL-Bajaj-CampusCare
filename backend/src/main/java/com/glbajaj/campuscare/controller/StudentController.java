package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.AnalyticsDtos.RoleDashboard;
import com.glbajaj.campuscare.dto.CommonDtos.PageResponse;
import com.glbajaj.campuscare.dto.IssueDtos.IssueSummary;
import com.glbajaj.campuscare.dto.IssueFilterParams;
import com.glbajaj.campuscare.security.CurrentUser;
import com.glbajaj.campuscare.service.DashboardService;
import com.glbajaj.campuscare.service.IssueService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final IssueService issueService;
    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    public StudentController(IssueService issueService, DashboardService dashboardService, CurrentUser currentUser) {
        this.issueService = issueService;
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public RoleDashboard dashboard() { return dashboardService.student(currentUser.require()); }

    /** The student's OWN issues only - scoping is enforced in IssueService.search. */
    @GetMapping("/issues")
    public PageResponse<IssueSummary> issues(@ModelAttribute IssueFilterParams params,
                                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return issueService.search(params.toFilter(), page, size, currentUser.require());
    }
}
