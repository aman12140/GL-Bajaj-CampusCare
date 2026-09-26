package com.glbajaj.campuscare.security;

import com.glbajaj.campuscare.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT (JSON Web Token) handling.
 *
 * How login works: the user sends email + password -> if correct, the server returns a signed token.
 * The browser sends this token in the "Authorization: Bearer ..." header on every later request.
 * The signature (HMAC-SHA256 with our secret key) proves the token was issued by this server and was not modified.
 */
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationHours;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours:12}") long expirationHours) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    /** Verifies signature and expiry. Throws a JwtException (subclass) when the token is invalid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
