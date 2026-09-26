package com.glbajaj.campuscare.security;

import com.glbajaj.campuscare.entity.Role;

/** The authenticated principal stored in the SecurityContext for each request. */
public record AuthUser(Long id, String email, Role role) {}
