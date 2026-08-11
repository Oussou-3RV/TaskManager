package com.orchestration.backend.config;

import com.orchestration.backend.security.JwtAuthFilter;
import com.orchestration.backend.tracing.TraceTimingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final TraceTimingFilter traceTimingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Les deux gestionnaires doivent etre explicites : sans eux, Spring Security
                // utilise Http403ForbiddenEntryPoint par defaut pour TOUT refus (pas connecte
                // OU role insuffisant), ce qui empeche le frontend de distinguer les deux cas.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Authentification requise ou expiree.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Role insuffisant pour cette action.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Machines — lecture pour tous les roles
                        .requestMatchers(HttpMethod.GET, "/api/v1/machines/**").hasAnyRole("OPERATOR", "ADMIN", "AUDITOR")
                        // Taches — operateur et admin
                        .requestMatchers("/api/v1/tasks/**").hasAnyRole("OPERATOR", "ADMIN")
                        // Historique des decisions de placement — lecture pour tous les roles
                        .requestMatchers(HttpMethod.GET, "/api/v1/placements/**").hasAnyRole("OPERATOR", "ADMIN", "AUDITOR")
                        // Regles — lecture pour tous, modification admin seulement
                        .requestMatchers(HttpMethod.GET, "/api/v1/rules/**").hasAnyRole("OPERATOR", "ADMIN", "AUDITOR")
                        .requestMatchers("/api/v1/rules/**").hasRole("ADMIN")
                        // Audit — auditeur et admin
                        .requestMatchers("/api/v1/audit/**").hasAnyRole("AUDITOR", "ADMIN")
                        // Tout le reste necessite authentification
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(traceTimingFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Sans ceci, le navigateur bloque la lecture JS de ces en-tetes cote frontend
        // (CORS ne les expose pas par defaut, meme sur une reponse "simple").
        config.setExposedHeaders(List.of("Server-Timing", "X-Trace-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}