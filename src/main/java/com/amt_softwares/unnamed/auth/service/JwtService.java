package com.amt_softwares.unnamed.auth.service;

import com.amt_softwares.unnamed.auth.config.JwtProperties;
import com.amt_softwares.unnamed.user.entity.User;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user, String role) {

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("phone", user.getPhone())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpiry()))
                .signWith(getSignKey(jwtProperties.getAccessSecret()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user, String role) {

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("phone", user.getPhone())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiry()))
                .signWith(getSignKey(jwtProperties.getRefreshSecret()), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey(String SECRET) {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public Claims extractAccessClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey(jwtProperties.getAccessSecret()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims extractRefreshClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey(jwtProperties.getRefreshSecret()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
