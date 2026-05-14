package com.amt_softwares.unnamed.admin.auth.controller;

import com.amt_softwares.unnamed.admin.auth.dto.AdminLoginRequest;
import com.amt_softwares.unnamed.admin.auth.dto.AdminTokenResponse;
import com.amt_softwares.unnamed.admin.auth.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AdminTokenResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
}
