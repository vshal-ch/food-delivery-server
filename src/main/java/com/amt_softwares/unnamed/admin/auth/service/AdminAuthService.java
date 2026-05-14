package com.amt_softwares.unnamed.admin.auth.service;

import com.amt_softwares.unnamed.admin.auth.dto.AdminLoginRequest;
import com.amt_softwares.unnamed.admin.auth.dto.AdminTokenResponse;
import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import com.amt_softwares.unnamed.admin.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminJwtService adminJwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminTokenResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        log.info("got here");

        if (!admin.isEnabled() || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        return AdminTokenResponse.builder()
                .accessToken(adminJwtService.generateAccessToken(admin))
                .expiresIn(adminJwtService.getAccessExpiry())
                .build();
    }
}
