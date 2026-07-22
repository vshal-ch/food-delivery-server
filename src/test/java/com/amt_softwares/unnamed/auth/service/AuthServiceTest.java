package com.amt_softwares.unnamed.auth.service;

import com.amt_softwares.unnamed.auth.entity.RefreshToken;
import com.amt_softwares.unnamed.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(refreshTokenRepository, jwtService);
    }

    @Test
    void logoutIgnoresBlankRefreshToken() {
        authService.logout("   ");

        verify(jwtService, never()).extractRefreshClaims(anyString());
        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void logoutIgnoresInvalidRefreshToken() {
        doThrow(new RuntimeException("bad token")).when(jwtService).extractRefreshClaims("bad-token");

        authService.logout("bad-token");

        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void logoutRevokesStoredTokenWhenRefreshTokenIsValid() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .token("valid-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));

        authService.logout("valid-token");

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(jwtService).extractRefreshClaims("valid-token");
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void logoutDoesNothingWhenValidTokenIsNotStored() {
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.empty());

        authService.logout("valid-token");

        verify(jwtService).extractRefreshClaims("valid-token");
        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any(RefreshToken.class));
    }
}
