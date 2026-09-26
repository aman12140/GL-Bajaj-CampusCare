package com.glbajaj.campuscare.dto;

import com.glbajaj.campuscare.entity.Role;
import com.glbajaj.campuscare.entity.UserStatus;
import com.glbajaj.campuscare.util.Validation;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String name,
            @NotBlank(message = "Student ID is required") @Size(max = 30) String studentId,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150) String email,
            @NotBlank(message = "Phone is required") @Pattern(regexp = Validation.PHONE, message = Validation.PHONE_MESSAGE) String phone,
            @NotBlank(message = "Course is required") @Size(max = 60) String course,
            @NotBlank(message = "Branch is required") @Size(max = 60) String branch,
            @NotNull(message = "Year is required") @Min(value = 1, message = "Year must be 1-6") @Max(value = 6, message = "Year must be 1-6") Integer year,
            @NotBlank(message = "Section is required") @Size(max = 10) String section,
            @NotBlank(message = "Password is required") @Pattern(regexp = Validation.PASSWORD, message = Validation.PASSWORD_MESSAGE) String password,
            @NotBlank(message = "Confirm your password") String confirmPassword) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record ForgotPasswordRequest(@NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email) {}

    public record ForgotPasswordResponse(String message, String resetLink) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = Validation.PASSWORD, message = Validation.PASSWORD_MESSAGE) String newPassword) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required") @Pattern(regexp = Validation.PASSWORD, message = Validation.PASSWORD_MESSAGE) String newPassword) {}

    /** Optional role-specific fields are ignored for roles they do not apply to. */
    public record UpdateProfileRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @Pattern(regexp = Validation.PHONE, message = Validation.PHONE_MESSAGE) String phone,
            @Size(max = 60) String course,
            @Size(max = 60) String branch,
            @Min(1) @Max(6) Integer year,
            @Size(max = 10) String section) {}

    /** Profile of the logged-in user. Never contains the password hash. */
    public record ProfileDto(Long id, String name, String email, String phone, Role role, UserStatus status,
                             boolean passwordChangeRequired,
                             String studentId, String course, String branch, Integer year, String section,
                             Long staffId, String employeeId, Long departmentId, String department, String designation) {}

    public record AuthResponse(String token, ProfileDto user) {}
}
