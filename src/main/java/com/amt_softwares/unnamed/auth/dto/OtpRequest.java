package com.amt_softwares.unnamed.auth.dto;

import lombok.Data;

@Data
public class OtpRequest {
    private String phone;
    private String role;
}
