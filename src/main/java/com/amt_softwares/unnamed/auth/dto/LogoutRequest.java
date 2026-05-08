package com.amt_softwares.unnamed.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
