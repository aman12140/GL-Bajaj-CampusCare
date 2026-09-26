package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.*;
import com.glbajaj.campuscare.util.EntityMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Admin-only staff management. Staff are activated / deactivated, never hard-deleted. */
@Service
public class StaffService {
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final IssueRepository issueRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EntityMapper mapper;

    public StaffService(StaffRepository staffRepository, UserRepository userRepository, DepartmentRepository departmentRepository,
                        IssueRepository issueRepository, PasswordEncoder passwordEncoder, NotificationService notificationService,
                        EntityMapper mapper) {
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.issueRepository = issueRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<StaffDto> list(Long departmentId, UserStatus status, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return staffRepository.findAll().stream()
                .filter(s -> departmentId == null || s.getDepartment().getId().equals(departmentId))
                .filter(s -> status == null || s.getUser().getStatus() == status)
                .filter(s -> needle.isEmpty() || s.getUser().getName().toLowerCase(Locale.ROOT).contains(needle)
                        || s.getEmployeeId().toLowerCase(Locale.ROOT).contains(needle)
                        || s.getUser().getEmail().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing((Staff s) -> s.getUser().getName()))
                .map(mapper::staff).toList();
    }

    /**
     * Admin creates a staff account with a TEMPORARY password. The password is BCrypt-hashed immediately
     * (the admin can never read it again) and the staff member must change it at first login.
     */
    @Transactional
    public StaffDto create(StaffCreateRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
        if (staffRepository.existsByEmployeeIdIgnoreCase(req.employeeId().trim())) throw ApiException.conflict("This Employee ID already exists.");
        Department dept = activeDepartment(req.departmentId());

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone().trim());
        user.setPassword(passwordEncoder.encode(req.temporaryPassword()));
        user.setRole(Role.STAFF);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordChangeRequired(true);
        user = userRepository.save(user);

        Staff staff = new Staff();
        staff.setUser(user);
        staff.setDepartment(dept);
        staff.setDesignation(req.designation().trim());
        staff.setEmployeeId(req.employeeId().trim().toUpperCase());
        return mapper.staff(staffRepository.save(staff));
    }

    @Transactional
    public StaffDto update(Long id, StaffUpdateRequest req) {
        Staff staff = find(id);
        String email = req.email().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            if (!u.getId().equals(staff.getUser().getId())) throw ApiException.conflict("An account with this email already exists.");
        });
        staffRepository.findByEmployeeIdIgnoreCase(req.employeeId().trim()).ifPresent(s -> {
            if (!s.getId().equals(id)) throw ApiException.conflict("This Employee ID already exists.");
        });
        Department dept = departmentRepository.findById(req.departmentId()).orElseThrow(() -> ApiException.notFound("Department not found"));
        staff.getUser().setName(req.name().trim());
        staff.getUser().setEmail(email);
        staff.getUser().setPhone(req.phone().trim());
        staff.setDepartment(dept);
        staff.setDesignation(req.designation().trim());
        staff.setEmployeeId(req.employeeId().trim().toUpperCase());
        return mapper.staff(staff);
    }

    /**
     * Deactivation = the staff member can no longer log in (checked at login AND on every request) and can no longer
     * receive new assignments. Existing assignments and all history stay in the database, so the admin can still see
     * them and reassign unresolved issues to active staff.
     */
    @Transactional
    public StaffDto deactivate(Long id) {
        Staff staff = find(id);
        User user = staff.getUser();
        if (user.getStatus() == UserStatus.INACTIVE) return mapper.staff(staff);
        user.setStatus(UserStatus.INACTIVE);
        long open = issueRepository.countByAssignedStaffIdAndStatusIn(id, SlaService.openStatuses());
        notificationService.notifyAdmins(null, "Staff deactivated",
                user.getName() + " (" + staff.getEmployeeId() + ") was deactivated. " + open + " open issue(s) may need reassignment.");
        return mapper.staff(staff);
    }

    @Transactional
    public StaffDto activate(Long id) {
        Staff staff = find(id);
        staff.getUser().setStatus(UserStatus.ACTIVE);
        return mapper.staff(staff);
    }

    private Department activeDepartment(Long id) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
        if (d.getStatus() != RecordStatus.ACTIVE) throw ApiException.badRequest("This department is inactive.");
        return d;
    }

    private Staff find(Long id) {
        return staffRepository.findById(id).orElseThrow(() -> ApiException.notFound("Staff member not found"));
    }
}
