package com.stackr.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_TYPE = "type";
    public  static final String TYPE_ACCESS  = "ACCESS";
    public  static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTtlMs;
    private final long refreshTtlMs = 30L * 24 * 60 * 60 * 1000; // 30 days

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl}") long accessTtlSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTtlMs = accessTtlSeconds * 1000;
    }

    public String generateAccessToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTtlMs))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTtlMs))
                .signWith(secretKey)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** Returns the token type claim ("ACCESS" or "REFRESH"), or null if invalid/absent. */
    public String getTokenType(String token) {
        try {
            return (String) parseClaims(token).get(CLAIM_TYPE);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTtlSeconds() {
        return accessTtlMs / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
