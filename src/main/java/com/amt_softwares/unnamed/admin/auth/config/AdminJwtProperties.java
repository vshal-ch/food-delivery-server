package com.amt_softwares.unnamed.admin.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.admin-jwt")
@Component
@Getter
@Setter
public class AdminJwtProperties {
    private String accessSecret;
    private long accessExpiry;
}
