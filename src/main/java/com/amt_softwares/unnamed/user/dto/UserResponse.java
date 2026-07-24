package com.amt_softwares.unnamed.user.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean isNew;
    private LocalDateTime createdAt;
}
