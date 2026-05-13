package com.amt_softwares.unnamed.admin.auth.service;

import com.amt_softwares.unnamed.admin.auth.config.AdminJwtProperties;
import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AdminJwtService {

    private final AdminJwtProperties adminJwtProperties;

    public String generateAccessToken(Admin admin) {
        return Jwts.builder()
                .setSubject(admin.getId().toString())
                .claim("username", admin.getUsername())
                .claim("role", "admin")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + adminJwtProperties.getAccessExpiry()))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getAccessExpiry() {
        return adminJwtProperties.getAccessExpiry();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(adminJwtProperties.getAccessSecret().getBytes());
    }
}
