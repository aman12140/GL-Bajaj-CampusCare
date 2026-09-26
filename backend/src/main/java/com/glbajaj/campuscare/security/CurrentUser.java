package com.glbajaj.campuscare.security;

import com.glbajaj.campuscare.entity.User;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Helper that returns the logged-in User entity for the current request. */
@Component
public class CurrentUser {
    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) { this.userRepository = userRepository; }

    public User require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser principal)) {
            throw ApiException.unauthorized("Authentication required. Please log in.");
        }
        return userRepository.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists."));
    }
}
