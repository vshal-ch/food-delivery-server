package com.amt_softwares.unnamed.admin.auth.service;

import com.amt_softwares.unnamed.admin.auth.dto.AdminLoginRequest;
import com.amt_softwares.unnamed.admin.auth.dto.AdminTokenResponse;
import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import com.amt_softwares.unnamed.admin.auth.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdminJwtService adminJwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthService(adminRepository, adminJwtService, passwordEncoder);
    }

    @Test
    void loginReturnsAccessTokenAndUpdatesLastLogin() {
        Admin admin = admin(true);
        AdminLoginRequest request = request("  root  ", "secret");

        when(adminRepository.findByUsername("root")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret", admin.getPassword())).thenReturn(true);
        when(adminJwtService.generateAccessToken(admin)).thenReturn("access-token");
        when(adminJwtService.getAccessExpiry()).thenReturn(900_000L);
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminTokenResponse response = adminAuthService.login(request);

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getLastLoginAt()).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getExpiresIn()).isEqualTo(900_000L);
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(adminRepository.findByUsername("root")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login(request("root", "secret")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void loginRejectsDisabledAdmin() {
        Admin admin = admin(false);

        when(adminRepository.findByUsername("root")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminAuthService.login(request("root", "secret")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        Admin admin = admin(true);

        when(adminRepository.findByUsername("root")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", admin.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login(request("root", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(adminRepository, never()).save(any(Admin.class));
    }

    private Admin admin(boolean enabled) {
        return Admin.builder()
                .id(UUID.randomUUID())
                .username("root")
                .password("encoded-password")
                .enabled(enabled)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private AdminLoginRequest request(String username, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
