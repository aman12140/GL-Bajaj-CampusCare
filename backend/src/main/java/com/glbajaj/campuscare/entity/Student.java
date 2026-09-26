package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "student_id", nullable = false, unique = true, length = 30)
    private String studentId;

    @Column(nullable = false, length = 60) private String course;
    @Column(nullable = false, length = 60) private String branch;

    /** Column is named study_year because YEAR is a reserved word in some databases. */
    @Column(name = "study_year", nullable = false)
    private Integer year;

    @Column(nullable = false, length = 10) private String section;
}
