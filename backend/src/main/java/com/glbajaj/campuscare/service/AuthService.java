package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AuthDtos.*;
import com.glbajaj.campuscare.dto.CommonDtos.MessageResponse;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.*;
import com.glbajaj.campuscare.security.JwtService;
import com.glbajaj.campuscare.util.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Registration, login, forgot/reset/change password and profile management. */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityMapper mapper;
    private final boolean exposeResetLink;
    private final String frontendUrl;

    public AuthService(UserRepository userRepository, StudentRepository studentRepository,
                       PasswordResetTokenRepository tokenRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       EntityMapper mapper, @Value("${app.expose-reset-link:false}") boolean exposeResetLink,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.exposeResetLink = exposeResetLink;
        this.frontendUrl = frontendUrl;
    }

    /** Students self-register. Staff and admins can NOT register here (role is always STUDENT). */
    @Transactional
    public MessageResponse register(RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) throw ApiException.badRequest("Password and confirm password do not match.");
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
        if (studentRepository.existsByStudentIdIgnoreCase(req.studentId().trim())) throw ApiException.conflict("This Student ID is already registered.");

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone().trim());
        user.setPassword(passwordEncoder.encode(req.password()));   // BCrypt hash, never the raw password
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentId(req.studentId().trim().toUpperCase());
        student.setCourse(req.course().trim());
        student.setBranch(req.branch().trim());
        student.setYear(req.year());
        student.setSection(req.section().trim().toUpperCase());
        studentRepository.save(student);
        return new MessageResponse("Registration successful. You can now log in.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password."));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid email or password.");
        }
        // Deactivated users (e.g. staff who left) can never log in.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden("Your account has been deactivated. Please contact the administrator.");
        }
        return new AuthResponse(jwtService.generateToken(user), mapper.profile(user));
    }

    /**
     * Forgot password: creates a random one-time token (valid 30 minutes). Only its SHA-256 hash is stored.
     * The response is identical whether the email exists or not (prevents account enumeration).
     * For the college demo no email is sent: the reset link is logged and, if app.expose-reset-link=true, returned.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req) {
        String generic = "If this email is registered, a password reset link has been generated.";
        User user = userRepository.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) return new ForgotPasswordResponse(generic, null);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        tokenRepository.save(token);

        String link = frontendUrl + "/reset-password?token=" + rawToken;
        if (exposeResetLink) {
            log.info("DEV ONLY - password reset link for {}: {}", user.getEmail(), link);
            return new ForgotPasswordResponse(generic + " (Development mode: use the link below.)", link);
        }
        return new ForgotPasswordResponse(generic, null);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(req.token()))
                .orElseThrow(() -> ApiException.badRequest("This reset link is invalid or has expired."));
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("This reset link is invalid or has expired.");
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setPasswordChangeRequired(false);
        token.setUsed(true);
        return new MessageResponse("Password updated. You can now log in with your new password.");
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found."));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) throw ApiException.badRequest("Current password is incorrect.");
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) throw ApiException.badRequest("New password must be different from the current password.");
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setPasswordChangeRequired(false);
        return new MessageResponse("Password changed successfully.");
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(Long userId) {
        return mapper.profile(userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found.")));
    }

    @Transactional
    public ProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found."));
        user.setName(req.name().trim());
        if (req.phone() != null && !req.phone().isBlank()) user.setPhone(req.phone().trim());
        if (user.getRole() == Role.STUDENT) {
            Student s = studentRepository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Student profile not found"));
            if (req.course() != null && !req.course().isBlank()) s.setCourse(req.course().trim());
            if (req.branch() != null && !req.branch().isBlank()) s.setBranch(req.branch().trim());
            if (req.year() != null) s.setYear(req.year());
            if (req.section() != null && !req.section().isBlank()) s.setSection(req.section().trim().toUpperCase());
        }
        // Staff designation/department/employee ID can only be changed by an admin (Staff Management).
        return mapper.profile(user);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
