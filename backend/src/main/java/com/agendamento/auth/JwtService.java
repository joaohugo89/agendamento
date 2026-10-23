package com.agendamento.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.agendamento.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String CLAIM_BUSINESS_ID = "bid";
    private static final String CLAIM_EMAIL = "email";

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(requireUsableSecret(properties.jwt().secret()));
        this.expiration = Duration.ofMinutes(properties.jwt().expirationMinutes());
    }

    /** Fail fast rather than sign tokens with a guessable key. */
    private static byte[] requireUsableSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long.");
        }
        if (secret.toLowerCase().contains("change-me")) {
            throw new IllegalStateException("JWT_SECRET still holds the placeholder value.");
        }
        return bytes;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_BUSINESS_ID, user.getBusinessId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return expiration.toSeconds();
    }

    /** Returns the principal encoded in the token, or {@code null} when the token is not usable. */
    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get(CLAIM_BUSINESS_ID, String.class)),
                    claims.get(CLAIM_EMAIL, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
