package com.glbajaj.campuscare.config;

import com.glbajaj.campuscare.repository.UserRepository;
import com.glbajaj.campuscare.security.JwtAuthenticationFilter;
import com.glbajaj.campuscare.security.JwtService;
import com.glbajaj.campuscare.security.SecurityErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Role-based authorization rules. This is the REAL security layer - the React route guards only hide menus.
 *
 * Rules are checked top-to-bottom; the first matching rule wins.
 * Ownership checks (a student may only open HIS issue, staff only ASSIGNED issues) are done again in the service layer.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorWriter errorWriter;
    private final String allowedOrigins;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository, SecurityErrorWriter errorWriter,
                          @Value("${app.cors-allowed-origins}") String allowedOrigins) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)                       // token-based API, no cookies -> CSRF not applicable
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> errorWriter.write(res, 401, "Authentication required. Please log in."))
                .accessDeniedHandler((req, res, ex) -> errorWriter.write(res, 403, "You do not have permission to perform this action.")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // ---- public ----
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()   // random UUID file names; <img> tags cannot send tokens
                .requestMatchers("/error").permitAll()
                // ---- admin only ----
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // ---- master data: everyone logged-in may read, only ADMIN may change ----
                .requestMatchers(HttpMethod.GET, "/api/departments/**", "/api/categories/**", "/api/locations/**",
                        "/api/location-types/**").authenticated()
                .requestMatchers("/api/departments/**", "/api/categories/**", "/api/locations/**", "/api/location-types/**").hasRole("ADMIN")
                // ---- role dashboards ----
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers("/api/staff/**").hasRole("STAFF")
                // ---- issues ----
                .requestMatchers(HttpMethod.POST, "/api/issues").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/issues").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/issues/suggest-priority").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/issues/*/assign").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/issues/*/status").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/issues/*/confirm", "/api/issues/*/reopen").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/issues/*/feedback").hasRole("STUDENT")
                .requestMatchers(HttpMethod.PUT, "/api/issues/*").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/issues/*").authenticated()   // ownership checked in IssueService
                // ---- everything else (profile, notifications, change-password ...) needs a valid login ----
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository, errorWriter),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
