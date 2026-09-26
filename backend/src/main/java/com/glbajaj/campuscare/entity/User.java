package com.glbajaj.campuscare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Login account for every role (STUDENT, STAFF, ADMIN). The password column stores a BCrypt hash only. */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_role", columnList = "role"))
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /** True for admin-created staff accounts until the staff member replaces the temporary password. */
    @Column(nullable = false)
    private boolean passwordChangeRequired = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
