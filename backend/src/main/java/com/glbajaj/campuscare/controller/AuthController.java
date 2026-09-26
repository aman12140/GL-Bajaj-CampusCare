package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.AuthDtos.*;
import com.glbajaj.campuscare.dto.CommonDtos.MessageResponse;
import com.glbajaj.campuscare.security.CurrentUser;
import com.glbajaj.campuscare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) { return authService.login(req); }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgot(@Valid @RequestBody ForgotPasswordRequest req) { return authService.forgotPassword(req); }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest req) { return authService.resetPassword(req); }

    @PostMapping("/change-password")
    public MessageResponse change(@Valid @RequestBody ChangePasswordRequest req) {
        return authService.changePassword(currentUser.require().getId(), req);
    }

    @GetMapping("/me")
    public ProfileDto me() { return authService.profile(currentUser.require().getId()); }
}
