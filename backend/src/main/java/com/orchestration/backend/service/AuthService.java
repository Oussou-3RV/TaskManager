package com.orchestration.backend.service;

import com.orchestration.backend.dto.request.LoginRequest;
import com.orchestration.backend.dto.response.AuthResponse;
import com.orchestration.backend.repository.UserRepository;
import com.orchestration.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_OPERATOR")
                .replace("ROLE_", "");

        String token = jwtUtils.generateToken(request.getUsername(), role);

        return AuthResponse.builder()
                .token(token)
                .username(request.getUsername())
                .role(role)
                .build();
    }
}