package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.user.entity.UserRole;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    private UserRoleService userRoleService;

    @BeforeEach
    void setUp() {
        userRoleService = new UserRoleService(userRoleRepository);
    }

    @Test
    void assignRoleSkipsSaveWhenRoleAlreadyExists() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.existsByUserIdAndRole(userId, "user")).thenReturn(true);

        userRoleService.assignRole(userId, "user");

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRoleSavesNewRoleWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.existsByUserIdAndRole(userId, "place")).thenReturn(false);

        userRoleService.assignRole(userId, "place");

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(userRoleCaptor.getValue().getRole()).isEqualTo("place");
        assertThat(userRoleCaptor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void getRolesReturnsRoleNamesOnly() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(
                UserRole.builder().id(UUID.randomUUID()).userId(userId).role("user").createdAt(LocalDateTime.now()).build(),
                UserRole.builder().id(UUID.randomUUID()).userId(userId).role("place").createdAt(LocalDateTime.now()).build()
        ));

        List<String> roles = userRoleService.getRoles(userId);

        assertThat(roles).containsExactly("user", "place");
    }
}
