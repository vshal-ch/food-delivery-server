package com.amt_softwares.unnamed.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserPlaceDetailResponse {
    private UUID id;
    private String displayName;
    private String description;
    private String phone;
    private boolean pureVeg;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean open;
    private List<UserMenuGroupResponse> menu;
}
