package com.amt_softwares.unnamed.auth.service;

import com.amt_softwares.unnamed.auth.config.JwtProperties;
import com.amt_softwares.unnamed.auth.dto.TokenResponse;
import com.amt_softwares.unnamed.auth.entity.RefreshToken;
import com.amt_softwares.unnamed.auth.repository.RefreshTokenRepository;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import com.amt_softwares.unnamed.user.service.UserRoleService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public TokenResponse generateTokens(User user, String role) {

        String accessToken = jwtService.generateAccessToken(user, role);

        String refreshToken = jwtService.generateRefreshToken(user, role);

        RefreshToken entity = RefreshToken.builder().id(UUID.randomUUID()).userId(user.getId()).token(refreshToken).expiresAt(LocalDateTime.now().plusDays(30)).revoked(false).createdAt(LocalDateTime.now()).build();

        refreshTokenRepository.save(entity);

        return new TokenResponse(accessToken, refreshToken, "Bearer", role);
    }

    public TokenResponse refresh(String refreshToken) {

        // 1. Parse JWT
        Claims claims;
        try {
            claims = jwtService.extractRefreshClaims(refreshToken);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        String role = claims.get("role", String.class);

        // 2. Fetch from DB
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // 3. Validate
        if (tokenEntity.isRevoked()) {
            throw new RuntimeException("Token revoked");
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        // 4. Rotate (revoke old token)
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // 5. Fetch user
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));


        // 7. Generate new tokens
        return generateTokens(user, role);
    }
}
