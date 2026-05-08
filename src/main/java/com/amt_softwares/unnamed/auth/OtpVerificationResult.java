package com.amt_softwares.unnamed.auth;

public enum OtpVerificationResult {
    SUCCESS,
    INVALID_OTP,
    OTP_EXPIRED,
    MAX_ATTEMPTS_EXCEEDED
}
