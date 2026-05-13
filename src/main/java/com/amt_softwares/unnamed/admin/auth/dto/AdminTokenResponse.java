package com.amt_softwares.unnamed.admin.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminTokenResponse {
    private String accessToken;
    private long expiresIn;
}
