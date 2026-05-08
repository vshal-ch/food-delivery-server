package com.amt_softwares.unnamed.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.jwt")
@Component
@Getter
@Setter
public class JwtProperties {
    private String accessSecret;
    private long accessExpiry;
    private String refreshSecret;
    private long refreshExpiry;
}
