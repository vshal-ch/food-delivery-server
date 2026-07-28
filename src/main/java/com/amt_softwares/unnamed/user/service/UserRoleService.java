package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.entity.UserRole;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public void assignRole(UUID userId, String role) {
        boolean exists = userRoleRepository.existsByUserIdAndRole(userId, role);
        if (exists) {
            return;
        }
        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .role(role)
                .isNew(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRoleRepository.save(userRole);
    }

    public List<String> getRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(UserRole::getRole)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserRole getUserRole(UUID userId, String role) {
        return userRoleRepository.findByUserIdAndRole(userId, role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User role not found"));
    }

    @Transactional
    public void setIsNewFalse(UUID userId, String role) {
        UserRole userRole = getUserRole(userId, role);
        userRole.setNew(false);
        userRoleRepository.save(userRole);
    }
}
