package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An operational (complaint-handling) department, e.g. "Electrical & Power". Not an academic department. */
@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
