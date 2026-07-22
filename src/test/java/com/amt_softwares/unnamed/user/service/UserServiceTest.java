package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getOrCreateUserReturnsExistingUser() {
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9999999999")
                .isNew(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.of(existingUser));

        User result = userService.getOrCreateUser("9999999999");

        assertThat(result).isSameAs(existingUser);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getOrCreateUserCreatesNewUserWhenMissing() {
        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.getOrCreateUser("9999999999");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPhone()).isEqualTo("9999999999");
        assertThat(savedUser.isNew()).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(result.getPhone()).isEqualTo("9999999999");
    }
}
