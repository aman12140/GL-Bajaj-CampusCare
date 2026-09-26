package com.glbajaj.campuscare.dto;

import com.glbajaj.campuscare.entity.LocationLevel;
import com.glbajaj.campuscare.entity.RecordStatus;
import com.glbajaj.campuscare.entity.UserStatus;
import com.glbajaj.campuscare.util.Validation;
import jakarta.validation.constraints.*;

public final class AdminDtos {
    private AdminDtos() {}

    // ---- staff ----
    public record StaffCreateRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String name,
            @NotBlank(message = "Employee ID is required") @Size(max = 30) String employeeId,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
            @NotBlank(message = "Phone is required") @Pattern(regexp = Validation.PHONE, message = Validation.PHONE_MESSAGE) String phone,
            @NotNull(message = "Department is required") Long departmentId,
            @NotBlank(message = "Designation is required") @Size(max = 100) String designation,
            @NotBlank(message = "Temporary password is required") @Pattern(regexp = Validation.PASSWORD, message = Validation.PASSWORD_MESSAGE) String temporaryPassword) {}

    public record StaffUpdateRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String name,
            @NotBlank(message = "Employee ID is required") @Size(max = 30) String employeeId,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
            @NotBlank(message = "Phone is required") @Pattern(regexp = Validation.PHONE, message = Validation.PHONE_MESSAGE) String phone,
            @NotNull(message = "Department is required") Long departmentId,
            @NotBlank(message = "Designation is required") @Size(max = 100) String designation) {}

    public record StaffDto(Long id, Long userId, String name, String employeeId, String email, String phone,
                           Long departmentId, String departmentName, String designation, UserStatus status,
                           long openAssignedIssues, long totalAssignedIssues) {}

    // ---- departments ----
    public record DepartmentRequest(@NotBlank(message = "Department name is required") @Size(max = 100) String name,
                                    @Size(max = 300) String description) {}

    public record DepartmentDto(Long id, String name, String description, RecordStatus status, long staffCount) {}

    // ---- categories ----
    public record CategoryRequest(@NotBlank(message = "Category name is required") @Size(max = 100) String name,
                                  @Size(max = 300) String description, Long parentId, Long departmentId) {}

    public record CategoryDto(Long id, String name, String description, Long parentId, String parentName,
                              Long departmentId, String departmentName, RecordStatus status) {}

    // ---- locations ----
    public record LocationRequest(@NotBlank(message = "Location name is required") @Size(max = 120) String name,
                                  String type, @NotNull(message = "Level is required") LocationLevel level,
                                  Long parentId, @Size(max = 40) String floor) {}

    public record LocationDto(Long id, String name, String type, LocationLevel level, Long parentId, String floor,
                              RecordStatus status, String path) {}

    public record LocationTypeRequest(@NotBlank(message = "Location type name is required") @Size(max = 80) String name) {}

    public record LocationTypeDto(Long id, String name, RecordStatus status) {}
}
