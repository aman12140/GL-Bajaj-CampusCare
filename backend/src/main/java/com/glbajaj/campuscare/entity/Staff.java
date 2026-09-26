package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Department staff profile. Staff are never hard-deleted: they are deactivated (user.status = INACTIVE). */
@Entity
@Table(name = "staff")
@Getter @Setter @NoArgsConstructor
public class Staff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(name = "employee_id", nullable = false, unique = true, length = 30)
    private String employeeId;
}
