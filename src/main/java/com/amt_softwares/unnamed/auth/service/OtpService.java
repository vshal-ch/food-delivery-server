package com.amt_softwares.unnamed.auth.service;

import com.amt_softwares.unnamed.auth.OtpVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "auth:otp:";
    private static final String ATTEMPT_PREFIX = "auth:otp:attempt:";

    private static final long OTP_TTL_MINS = 5; // minutes
    private static final int MAX_ATTEMPTS = 3;

    public void sendOtp(String phone, String role) {
        String otp = "123456"; // hardcoded

        redisTemplate.opsForValue().set(
                OTP_PREFIX + role + ":" + phone,
                otp,
                OTP_TTL_MINS, TimeUnit.MINUTES
        );

        redisTemplate.delete(ATTEMPT_PREFIX + role + ":" + phone);
        log.info("OTP for {} is {}", phone, otp);
    }

    public OtpVerificationResult verifyOtp(String phone, String role, String otp) {
        String key = OTP_PREFIX + role + ":" + phone;
        String attemptKey = ATTEMPT_PREFIX + role + ":" + phone;

        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);

        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(key);
            return OtpVerificationResult.MAX_ATTEMPTS_EXCEEDED;
        }

        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return OtpVerificationResult.OTP_EXPIRED;
        }

        if (!storedOtp.equals(otp)) {
            Long newAttempts = redisTemplate.opsForValue().increment(attemptKey);
            if (newAttempts != null && newAttempts == 1) {
                redisTemplate.expire(attemptKey, 5, TimeUnit.MINUTES);
            }

            return OtpVerificationResult.INVALID_OTP;
        }

        redisTemplate.delete(key); // one-time use
        redisTemplate.delete(attemptKey);

        return OtpVerificationResult.SUCCESS;
    }
}