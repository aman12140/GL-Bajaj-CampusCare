package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Timeline of an issue. Every important event (report, assign, start, resolve, confirm, reopen ...) adds a row. */
@Entity
@Table(name = "issue_status_history", indexes = @Index(name = "idx_history_issue", columnList = "issue_id"))
@Getter @Setter @NoArgsConstructor
public class IssueStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Enumerated(EnumType.STRING) @Column(name = "old_status", length = 20)
    private IssueStatus oldStatus;

    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20)
    private IssueStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(length = 1000)
    private String comment;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
