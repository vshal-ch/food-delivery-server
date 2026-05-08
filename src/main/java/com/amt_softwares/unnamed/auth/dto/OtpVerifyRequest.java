package com.amt_softwares.unnamed.auth.dto;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String phone;
    private String otp;
    private String role;
}