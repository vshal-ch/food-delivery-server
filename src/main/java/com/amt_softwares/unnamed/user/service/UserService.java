package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getOrCreateUser(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> createUser(phone));
    }

    private User createUser(String phone) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .phone(phone)
                .isNew(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }
}
