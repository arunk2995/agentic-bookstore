package com.bookstore.security;

import com.bookstore.config.BookstoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private BookstoreProperties properties;

    @BeforeEach
    void setup() {
        properties = new BookstoreProperties();
        properties.getJwt().setSecret("test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        properties.getJwt().setAccessTokenExpiry(3600);
        properties.getJwt().setRefreshTokenExpiry(86400);
        properties.getJwt().setIssuer("bookstore-test");
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("generateAccessToken creates valid token")
    void generateAccessToken_createsValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "testuser", "test@example.com");

        assertThat(token).isNotNull().isNotBlank();
        assertThat(jwtTokenProvider.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("extractUserId returns correct UUID")
    void extractUserId_returnsCorrectId() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "testuser", "test@example.com");

        UUID extracted = jwtTokenProvider.extractUserId(token);
        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    @DisplayName("isValid returns false for tampered token")
    void isValid_tamperedToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "testuser", "test@example.com");
        String tampered = token + "tampered";

        assertThat(jwtTokenProvider.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for empty token")
    void isValid_emptyToken_returnsFalse() {
        assertThat(jwtTokenProvider.isValid("")).isFalse();
        assertThat(jwtTokenProvider.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("generateRefreshToken creates different token from access token")
    void generateRefreshAndAccessToken_areDifferent() {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtTokenProvider.generateAccessToken(userId, "user", "user@test.com");
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, "user", "user@test.com");

        assertThat(accessToken).isNotEqualTo(refreshToken);
        assertThat(jwtTokenProvider.isValid(refreshToken)).isTrue();
    }
}
