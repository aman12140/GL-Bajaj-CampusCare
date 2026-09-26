package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.CommonDtos.PageResponse;
import com.glbajaj.campuscare.dto.IssueDtos.*;
import com.glbajaj.campuscare.dto.IssueFilterParams;
import com.glbajaj.campuscare.security.CurrentUser;
import com.glbajaj.campuscare.service.IssueService;
import com.glbajaj.campuscare.service.PrioritySuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Issue endpoints. URL-level role rules are in SecurityConfig; ownership rules are in IssueService. */
@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService issueService;
    private final PrioritySuggestionService prioritySuggestionService;
    private final CurrentUser currentUser;

    public IssueController(IssueService issueService, PrioritySuggestionService prioritySuggestionService, CurrentUser currentUser) {
        this.issueService = issueService;
        this.prioritySuggestionService = prioritySuggestionService;
        this.currentUser = currentUser;
    }

    /** ADMIN: all issues (filterable, paged). */
    @GetMapping
    public PageResponse<IssueSummary> list(@ModelAttribute IssueFilterParams params,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return issueService.search(params.toFilter(), page, size, currentUser.require());
    }

    /** STUDENT: report an issue. Multipart form: text fields + optional "photo" (JPG/PNG, max 5 MB). */
    @PostMapping
    public ResponseEntity<IssueDetail> create(@Valid @ModelAttribute IssueCreateRequest req,
                                              @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.create(currentUser.require(), req, photo));
    }

    @PostMapping("/suggest-priority")
    public PrioritySuggestion suggest(@RequestBody SuggestPriorityRequest req) {
        return prioritySuggestionService.suggest(req.title(), req.description(), req.categoryId());
    }

    @GetMapping("/{id}")
    public IssueDetail get(@PathVariable Long id) { return issueService.get(id, currentUser.require()); }

    @PutMapping("/{id}")
    public IssueDetail update(@PathVariable Long id, @Valid @RequestBody IssueUpdateRequest req) {
        return issueService.update(id, req, currentUser.require());
    }

    @PutMapping("/{id}/assign")
    public IssueDetail assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return issueService.assign(id, req, currentUser.require());
    }

    /** STAFF/ADMIN: IN_PROGRESS, RESOLVED (comment = resolution note, optional "photo"), or ADMIN REJECTED (comment = reason). */
    @PutMapping("/{id}/status")
    public IssueDetail status(@PathVariable Long id, @Valid @ModelAttribute StatusUpdateRequest req,
                              @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return issueService.changeStatus(id, req, photo, currentUser.require());
    }

    @PutMapping("/{id}/confirm")
    public IssueDetail confirm(@PathVariable Long id) { return issueService.confirm(id, currentUser.require()); }

    @PutMapping("/{id}/reopen")
    public IssueDetail reopen(@PathVariable Long id, @Valid @RequestBody ReopenRequest req) {
        return issueService.reopen(id, req, currentUser.require());
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<FeedbackDto> feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.feedback(id, req, currentUser.require()));
    }
}
