package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.entity.UserRole;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public void assignRole(UUID userId, String role) {
        boolean exists = userRoleRepository.existsByUserIdAndRole(userId, role);
        if(exists){
            return;
        }
        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .role(role)
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
}
