package com.example.bestpractices.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Best practices demonstrated:
 * - Declare a SecurityFilterChain @Bean instead of extending WebSecurityConfigurerAdapter (removed in Spring Boot 3)
 * - Disable CSRF for stateless REST APIs (JWT/token-based auth carries its own CSRF protection)
 * - Use STATELESS session policy — the server never creates an HttpSession
 * - Allow public access to actuator health, OpenAPI docs, and H2 console explicitly;
 *   lock down everything else
 * - Separate read (GET) from write (POST/PUT/PATCH/DELETE) permissions for fine-grained RBAC
 *
 * NOTE: In a real application replace the in-memory authentication with a UserDetailsService
 * backed by the database and add JWT filter before UsernamePasswordAuthenticationFilter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Read operations are open; writes require authentication
                .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                .anyRequest().authenticated()
            )
            // Allow H2 console frames (dev only — remove in production)
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }
}
