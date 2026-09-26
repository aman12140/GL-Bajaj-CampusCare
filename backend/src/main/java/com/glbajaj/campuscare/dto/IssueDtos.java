package com.glbajaj.campuscare.dto;

import com.glbajaj.campuscare.entity.IssueStatus;
import com.glbajaj.campuscare.entity.Priority;
import com.glbajaj.campuscare.util.Validation;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class IssueDtos {
    private IssueDtos() {}

    /** Bound from multipart form fields (photo travels separately). */
    public record IssueCreateRequest(
            @NotBlank(message = "Issue title is required") @Size(min = 5, max = 150, message = "Title must be 5-150 characters") String title,
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Location is required") Long locationId,
            @NotNull(message = "Priority is required") Priority priority,
            @NotBlank(message = "Description is required") @Size(min = 10, max = 2000, message = "Description must be 10-2000 characters") String description,
            @Pattern(regexp = Validation.PHONE, message = Validation.PHONE_MESSAGE) String contactNumber,
            @Pattern(regexp = Validation.ROOM, message = Validation.ROOM_MESSAGE) String roomNumber) {}

    public record IssueUpdateRequest(
            @Size(min = 5, max = 150, message = "Title must be 5-150 characters") String title,
            @Size(min = 10, max = 2000, message = "Description must be 10-2000 characters") String description,
            Priority priority, Long categoryId, Long locationId,
            @Pattern(regexp = Validation.PHONE, message = Validation.PHONE_MESSAGE) String contactNumber,
            @Pattern(regexp = Validation.ROOM, message = Validation.ROOM_MESSAGE) String roomNumber) {}

    public record AssignRequest(@NotNull(message = "Select a staff member") Long staffId,
                                @Size(max = 500) String comment) {}

    /** Bound from multipart form fields: status + comment (resolution note / rejection reason), optional photo separately. */
    public record StatusUpdateRequest(@NotNull(message = "Status is required") IssueStatus status,
                                      @Size(max = 1000) String comment) {}

    public record ReopenRequest(@NotBlank(message = "Please tell us why the issue is not resolved")
                                @Size(min = 5, max = 500, message = "Reason must be 5-500 characters") String reason) {}

    public record FeedbackRequest(@NotNull(message = "Rating is required") @Min(1) @Max(5) Integer rating,
                                  @Size(max = 1000) String comment) {}

    public record SuggestPriorityRequest(String title, String description, Long categoryId) {}

    public record PrioritySuggestion(Priority priority, List<String> reasons, String note) {}

    public record TimelineEntry(String oldStatus, String newStatus, String actorName, String actorRole,
                                String comment, LocalDateTime changedAt) {}

    public record FeedbackDto(int rating, String comment, LocalDateTime createdAt) {}

    public record StaffRef(Long id, String name, String employeeId, String designation, String status) {}

    public record IssueSummary(Long id, String issueNumber, String title, String categoryName, String locationPath,
                               Priority priority, IssueStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                               LocalDateTime resolvedAt, String studentName, String assignedStaffName,
                               String assignedDepartment, boolean overdue) {}

    public record IssueDetail(Long id, String issueNumber, String title, String description,
                              Long categoryId, String categoryPath, Long locationId, String locationPath, String roomNumber,
                              Priority priority, IssueStatus status, String imageUrl, String contactNumber,
                              String studentName, String studentId, String studentEmail, String studentPhone,
                              StaffRef assignedStaff, Long assignedDepartmentId, String assignedDepartment,
                              Long suggestedDepartmentId, String suggestedDepartment,
                              String resolutionNote, String resolutionImageUrl,
                              LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt, LocalDateTime closedAt,
                              int slaHours, LocalDateTime dueAt, boolean overdue,
                              FeedbackDto feedback, List<TimelineEntry> timeline) {}
}
