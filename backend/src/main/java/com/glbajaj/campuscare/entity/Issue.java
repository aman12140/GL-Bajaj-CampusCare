package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A reported campus issue.
 * "assignedStaff/assignedDepartment" are the CURRENT assignment
 * (history is in issue_assignments).
 */
@Entity
@Table(name = "issues", indexes = {
        @Index(name = "idx_issues_status", columnList = "status"),
        @Index(name = "idx_issues_priority", columnList = "priority"),
        @Index(name = "idx_issues_created", columnList = "created_at"),
        @Index(name = "idx_issues_student", columnList = "student_id"),
        @Index(name = "idx_issues_staff", columnList = "assigned_staff_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human readable number such as CC-1001.
     */
    @Column(name = "issue_number", nullable = false, unique = true, length = 40)
    private String issueNumber;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Free-text room number typed by the student (optional),
     * e.g. "204" or "Lab 3".
     */
    @Column(name = "room_number", length = 40)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status = IssueStatus.REPORTED;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(length = 20)
    private String contactNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Staff assignedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_department_id")
    private Department assignedDepartment;

    @Column(length = 1000)
    private String resolutionNote;

    @Column(length = 300)
    private String resolutionImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }
}