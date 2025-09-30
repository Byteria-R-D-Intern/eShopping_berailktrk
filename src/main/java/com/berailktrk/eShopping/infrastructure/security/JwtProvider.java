package com.berailktrk.eShopping.infrastructure.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.berailktrk.eShopping.domain.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT token oluşturma ve doğrulama servisi
 */
@Component
public class JwtProvider {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationThatShouldBeLongEnoughAndSecure12345}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 saat (milisaniye)
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Kullanıcı için JWT token oluşturur
     * 
     * @param user kullanıcı
     * @return JWT token
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Token'dan email çıkarır
     * 
     * @param token JWT token
     * @return email
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Token'dan user ID çıkarır
     * 
     * @param token JWT token
     * @return user ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.get("userId", String.class));
    }

    /**
     * Token'ı doğrular
     * 
     * @param token JWT token
     * @return geçerliyse true
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Token süresini döndürür (milisaniye)
     * 
     * @return token süresi
     */
    public Long getJwtExpiration() {
        return jwtExpiration;
    }
}
