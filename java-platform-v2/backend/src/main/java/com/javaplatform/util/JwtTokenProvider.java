package com.javaplatform.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret:mysecretkeyforjwttoken}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    /**
     * Generate JWT token
     */
    public String generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(SignatureAlgorithm.HS512, jwtSecret.getBytes(StandardCharsets.UTF_8))
            .compact();
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
            return (String) claims.get("username");
        } catch (JwtException e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }
}
