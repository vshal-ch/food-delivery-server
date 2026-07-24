package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.dto.UpdateUserProfileRequest;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userRoleRepository);
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

    @Test
    void updateProfileUpdatesOnlyProvidedFields() {
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now().minusHours(2);
        User user = User.builder()
                .id(userId)
                .phone("9999999999")
                .name("Old Name")
                .email("old@example.com")
                .isNew(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail("  new@example.com  ");

        when(userRoleRepository.existsByUserIdAndRole(userId, "user")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(userId, "user", request);

        assertThat(result.getName()).isEqualTo("Old Name");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.isNew()).isFalse();
        assertThat(result.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    void updateProfileRejectsRequestWhenNameAndEmailMissing() {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("   ");

        when(userRoleRepository.existsByUserIdAndRole(userId, "user")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder()
                        .id(userId)
                        .phone("9999999999")
                        .isNew(true)
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .updatedAt(LocalDateTime.now().minusHours(1))
                        .build()
        ));

        assertThatThrownBy(() -> userService.updateProfile(userId, "user", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo("Either name or email is required");
                });
    }

    @Test
    void updateProfileRejectsNonUserRole() {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("Vishal");

        assertThatThrownBy(() -> userService.updateProfile(userId, "place", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(responseStatusException.getReason()).isEqualTo("Only user role can update profile");
                });
    }
}
