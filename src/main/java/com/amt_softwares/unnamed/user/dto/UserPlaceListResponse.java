package com.amt_softwares.unnamed.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class UserPlaceListResponse {
    private UUID id;
    private String displayName;
    private String description;
    private boolean pureVeg;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
