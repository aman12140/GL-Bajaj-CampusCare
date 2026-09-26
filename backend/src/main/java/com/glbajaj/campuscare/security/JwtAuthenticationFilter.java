package com.glbajaj.campuscare.security;

import com.glbajaj.campuscare.entity.User;
import com.glbajaj.campuscare.entity.UserStatus;
import com.glbajaj.campuscare.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Runs once for every request. If a valid Bearer token is present it loads the user FROM THE DATABASE and
 * puts an authenticated principal into the SecurityContext.
 *
 * Because the user is re-loaded on each request, deactivating a staff member takes effect immediately:
 * an INACTIVE user is never authenticated, even if their old token has not expired yet.
 *
 * It also enforces "change your temporary password first": while passwordChangeRequired is true, only the
 * profile/change-password endpoints work.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, SecurityErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                Optional<User> found = userRepository.findByEmailIgnoreCase(claims.getSubject());
                if (found.isPresent() && found.get().getStatus() == UserStatus.ACTIVE) {
                    User user = found.get();
                    if (user.isPasswordChangeRequired() && !isAllowedWhilePasswordChangePending(request)) {
                        errorWriter.write(response, 403, "You must change your temporary password before continuing.");
                        return;
                    }
                    AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid / expired token: leave the context empty -> the entry point answers 401.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowedWhilePasswordChangePending(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/change-password") || path.equals("/api/auth/me") || path.equals("/api/profile");
    }
}
