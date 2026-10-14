package com.bookstore.security;

import com.bookstore.config.BookstoreProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final BookstoreProperties properties;

    public JwtTokenProvider(BookstoreProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwt().getSecret().getBytes();
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String username, String email) {
        return generateToken(userId, username, email,
            properties.getJwt().getAccessTokenExpiry() * 1000L, "access");
    }

    public String generateRefreshToken(UUID userId, String username, String email) {
        return generateToken(userId, username, email,
            properties.getJwt().getRefreshTokenExpiry() * 1000L, "refresh");
    }

    private String generateToken(UUID userId, String username, String email,
                                  long expiryMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
            .issuer(properties.getJwt().getIssuer())
            .subject(userId.toString())
            .claim("username", username)
            .claim("email", email)
            .claim("type", tokenType)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    public Claims validateAndParseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(properties.getJwt().getIssuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            validateAndParseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = validateAndParseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String extractUsername(String token) {
        return validateAndParseClaims(token).get("username", String.class);
    }
}
