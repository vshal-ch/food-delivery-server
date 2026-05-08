package com.amt_softwares.unnamed.auth.controller;

import com.amt_softwares.unnamed.auth.OtpVerificationResult;
import com.amt_softwares.unnamed.auth.dto.*;
import com.amt_softwares.unnamed.auth.service.AuthService;
import com.amt_softwares.unnamed.auth.service.OtpService;
import com.amt_softwares.unnamed.auth.service.TokenService;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.service.UserRoleService;
import com.amt_softwares.unnamed.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final TokenService tokenService;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
        otpService.sendOtp(request.getPhone(),request.getRole());
        return ResponseEntity.ok("OTP sent");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {

        OtpVerificationResult result = otpService.verifyOtp(
                request.getPhone(),
                request.getRole(),
                request.getOtp()
        );

        switch (result) {

            case MAX_ATTEMPTS_EXCEEDED:
                return ResponseEntity.status(429).body("Too many attempts");

            case OTP_EXPIRED:
                return ResponseEntity.badRequest().body("OTP expired");

            case INVALID_OTP:
                return ResponseEntity.badRequest().body("Invalid OTP");

            case SUCCESS:
                User user = userService.getOrCreateUser(request.getPhone());

                // checks for existing and creates new row in user_roles table if it doesn't exist
                userRoleService.assignRole(user.getId(), request.getRole());

                TokenResponse tokens = tokenService.generateTokens(user, request.getRole());

                return ResponseEntity.ok(tokens);
        }

        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        TokenResponse response = tokenService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}
