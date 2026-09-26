package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.AuthDtos.ProfileDto;
import com.glbajaj.campuscare.dto.AuthDtos.UpdateProfileRequest;
import com.glbajaj.campuscare.security.CurrentUser;
import com.glbajaj.campuscare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AuthService authService;
    private final CurrentUser currentUser;

    public ProfileController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ProfileDto get() { return authService.profile(currentUser.require().getId()); }

    @PutMapping
    public ProfileDto update(@Valid @RequestBody UpdateProfileRequest req) {
        return authService.updateProfile(currentUser.require().getId(), req);
    }
}
