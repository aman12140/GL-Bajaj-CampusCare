package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.entity.Issue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Builds the database query (WHERE clause) from an IssueFilter. */
public final class IssueSpecifications {
    private IssueSpecifications() {}

    public static Specification<Issue> from(IssueFilter f, Set<Long> categoryIds, Set<Long> locationIds) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.statuses() != null && !f.statuses().isEmpty()) p.add(root.get("status").in(f.statuses()));
            if (f.priority() != null) p.add(cb.equal(root.get("priority"), f.priority()));
            if (categoryIds != null) p.add(root.get("category").get("id").in(categoryIds));
            if (locationIds != null) p.add(root.get("location").get("id").in(locationIds));
            if (f.departmentId() != null) p.add(cb.equal(root.get("assignedDepartment").get("id"), f.departmentId()));
            if (f.staffId() != null) p.add(cb.equal(root.get("assignedStaff").get("id"), f.staffId()));
            if (f.studentId() != null) p.add(cb.equal(root.get("student").get("id"), f.studentId()));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), f.from()));
            if (f.to() != null) p.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), f.to()));
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.<String>get("title")), like),
                        cb.like(cb.lower(root.<String>get("issueNumber")), like),
                        cb.like(cb.lower(root.<String>get("description")), like),
                        cb.like(cb.lower(root.<String>get("roomNumber")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
