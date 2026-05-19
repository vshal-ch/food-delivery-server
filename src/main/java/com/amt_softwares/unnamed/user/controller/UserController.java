package com.amt_softwares.unnamed.user.controller;

import com.amt_softwares.unnamed.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(Map.of("message", "logged in", "phone", user.getPhone(), " with role", role));
    }
}
