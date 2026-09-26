package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One row per assignment. unassignedAt stays null while the assignment is current. */
@Entity
@Table(name = "issue_assignments", indexes = @Index(name = "idx_assign_issue", columnList = "issue_id"))
@Getter @Setter @NoArgsConstructor
public class IssueAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;
}
