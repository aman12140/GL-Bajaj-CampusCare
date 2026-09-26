package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.dto.AnalyticsDtos.AnalyticsResponse;
import com.glbajaj.campuscare.dto.IssueFilterParams;
import com.glbajaj.campuscare.entity.UserStatus;
import com.glbajaj.campuscare.service.AnalyticsService;
import com.glbajaj.campuscare.service.ExportService;
import com.glbajaj.campuscare.service.IssueFilter;
import com.glbajaj.campuscare.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Everything under /api/admin/** is restricted to the ADMIN role in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AnalyticsService analyticsService;
    private final ExportService exportService;
    private final StaffService staffService;

    public AdminController(AnalyticsService analyticsService, ExportService exportService, StaffService staffService) {
        this.analyticsService = analyticsService;
        this.exportService = exportService;
        this.staffService = staffService;
    }

    @GetMapping("/dashboard")
    public AnalyticsResponse dashboard() { return analyticsService.compute(IssueFilter.none(), "All time"); }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics(@ModelAttribute IssueFilterParams params) {
        return analyticsService.compute(params.toFilter(), params.rangeLabel());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> csv(@ModelAttribute IssueFilterParams params) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campuscare-issues-" + LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(exportService.csv(params.toFilter()));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> pdf(@ModelAttribute IssueFilterParams params) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campuscare-report-" + LocalDate.now() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(exportService.pdf(params.toFilter(), params.rangeLabel()));
    }

    // ---------------- staff management ----------------
    @GetMapping("/staff")
    public List<StaffDto> staff(@RequestParam(required = false) Long departmentId, @RequestParam(required = false) UserStatus status,
                                @RequestParam(required = false) String q) {
        return staffService.list(departmentId, status, q);
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffDto> createStaff(@Valid @RequestBody StaffCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(req));
    }

    @PutMapping("/staff/{id}")
    public StaffDto updateStaff(@PathVariable Long id, @Valid @RequestBody StaffUpdateRequest req) { return staffService.update(id, req); }

    @PutMapping("/staff/{id}/deactivate")
    public StaffDto deactivate(@PathVariable Long id) { return staffService.deactivate(id); }

    @PutMapping("/staff/{id}/activate")
    public StaffDto activate(@PathVariable Long id) { return staffService.activate(id); }
}
