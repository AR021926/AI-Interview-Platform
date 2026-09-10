package com.interview.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final long EXPIRATION_TIME =
            24 * 60 * 60 * 1000L;

    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION_TIME
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public boolean isTokenValid(
            String token,
            String email
    ) {

        String tokenEmail =
                extractEmail(token);

        return tokenEmail.equals(email)
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(
            String token
    ) {

        return extractExpiration(token)
                .before(new Date());
    }

    private Date extractExpiration(
            String token
    ) {

        return extractClaim(
                token,
                Claims::getExpiration
        );
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        Claims claims =
                extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(
            String token
    ) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {

        String secret =
                System.getenv("JWT_SECRET");

        if (secret == null
                || secret.isBlank()) {

            throw new IllegalStateException(
                    "JWT_SECRET environment variable is not configured"
            );
        }

        if (secret.length() < 32) {

            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 characters long"
            );
        }

        return Keys.hmacShaKeyFor(
                secret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}