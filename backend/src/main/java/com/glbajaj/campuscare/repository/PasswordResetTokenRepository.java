package com.glbajaj.campuscare.repository;

import com.glbajaj.campuscare.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
