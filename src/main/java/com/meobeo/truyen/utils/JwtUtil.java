package com.meobeo.truyen.utils;

import com.meobeo.truyen.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtConfig jwtConfig;

    /**
     * Tạo access token với thông tin user
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, jwtConfig.getAccessToken().getExpiration());
    }

    /**
     * Tạo refresh token với thông tin user
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, jwtConfig.getRefreshToken().getExpiration());
    }

    /**
     * Tạo token với payload và thời gian hết hạn
     */
    private String generateToken(Long userId, String username, String role, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Xác thực token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token không hợp lệ: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lấy userId từ token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Lấy username từ token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Lấy role từ token
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Kiểm tra token có hết hạn chưa
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Lấy claims từ token
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Tạo signing key từ secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}