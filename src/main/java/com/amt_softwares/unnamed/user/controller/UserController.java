package com.amt_softwares.unnamed.user.controller;

import com.amt_softwares.unnamed.user.dto.UpdateUserProfileRequest;
import com.amt_softwares.unnamed.user.dto.UserResponse;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        return ResponseEntity.ok(toResponse(user, role));
    }

    @PutMapping("/user/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
        Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        User updatedUser = userService.updateProfile(user.getId(), role, request);
        return ResponseEntity.ok(toResponse(updatedUser, role));
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority();
    }

    private UserResponse toResponse(User user, String role) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isNew(user.isNew())
                .role(role)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
