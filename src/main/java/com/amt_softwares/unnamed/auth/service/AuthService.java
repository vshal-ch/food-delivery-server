package com.amt_softwares.unnamed.auth.service;

import com.amt_softwares.unnamed.auth.entity.RefreshToken;
import com.amt_softwares.unnamed.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        // Validate JWT
        try {
            jwtService.extractRefreshClaims(refreshToken);
        } catch (Exception e) {
            return;
        }

        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

    }
}