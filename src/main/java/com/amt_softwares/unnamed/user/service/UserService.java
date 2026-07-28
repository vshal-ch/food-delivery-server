package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.dto.UpdateUserProfileRequest;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.entity.UserRole;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String USER_ROLE = "user";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public User getOrCreateUser(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> createUser(phone));
    }

    @Transactional
    public User updateProfile(UUID userId, String authenticatedRole, UpdateUserProfileRequest request) {
        verifyUserRole(userId, authenticatedRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String cleanedName = cleanOptional(request.getName());
        String cleanedEmail = cleanOptional(request.getEmail());

        if (cleanedName == null && cleanedEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either name or email is required");
        }

        if (cleanedName != null) {
            user.setName(cleanedName);
        }

        if (cleanedEmail != null) {
            user.setEmail(cleanedEmail);
        }

        UserRole userRole = userRoleRepository.findByUserIdAndRole(userId, USER_ROLE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User role not found"));
        userRole.setNew(false);
        user.setUpdatedAt(LocalDateTime.now());

        userRoleRepository.save(userRole);
        return userRepository.save(user);
    }

    private User createUser(String phone) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .phone(phone)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private void verifyUserRole(UUID userId, String authenticatedRole) {
        if (!USER_ROLE.equals(authenticatedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only user role can update profile");
        }

        if (!userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have user role");
        }
    }

    private String cleanOptional(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
