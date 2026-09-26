package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter @Setter @NoArgsConstructor
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** unique = only one feedback per issue. */
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id", nullable = false, unique = true)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
