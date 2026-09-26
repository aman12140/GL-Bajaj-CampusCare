package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Issue category. Top-level categories have parent == null; subcategories point to their parent.
 * Each (sub)category can suggest a department, used as the default when the admin assigns an issue.
 */
@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
