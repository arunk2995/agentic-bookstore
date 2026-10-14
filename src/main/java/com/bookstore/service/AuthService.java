package com.bookstore.service;

import com.bookstore.config.BookstoreProperties;
import com.bookstore.domain.user.Role;
import com.bookstore.domain.user.RoleName;
import com.bookstore.domain.user.User;
import com.bookstore.domain.user.UserStatus;
import com.bookstore.dto.auth.*;
import com.bookstore.exception.BookstoreException;
import com.bookstore.repository.RoleRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.BookstoreUserPrincipal;
import com.bookstore.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final BookstoreProperties properties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BookstoreException.conflict("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BookstoreException.conflict("Username already taken: " + request.getUsername());
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
            .orElseThrow(() -> new IllegalStateException("CUSTOMER role not found"));

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .status(UserStatus.ACTIVE)
            .build();
        user.addRole(customerRole);

        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        BookstoreUserPrincipal principal = (BookstoreUserPrincipal) auth.getPrincipal();
        User user = principal.getUser();

        userRepository.resetFailedLoginAttempts(user.getId());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isValid(request.getRefreshToken())) {
            throw BookstoreException.unauthorized("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.extractUserId(request.getRefreshToken());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BookstoreException.unauthorized("User not found"));

        if (!user.isActive()) {
            throw BookstoreException.unauthorized("Account is not active");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            user.getId(), user.getUsername(), user.getEmail());

        List<String> roles = user.getRoles().stream()
            .map(r -> r.getName().name())
            .toList();

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(properties.getJwt().getAccessTokenExpiry())
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .build())
            .build();
    }

    public BookstoreUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw BookstoreException.unauthorized("Not authenticated");
        }
        return (BookstoreUserPrincipal) auth.getPrincipal();
    }
}
