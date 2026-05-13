package com.amt_softwares.unnamed.place.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PlaceResponse {
    private UUID id;
    private UUID changeId;
    private UUID ownerId;
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
    private boolean hidden;
    private String approvalStatus;
    private String rejectionReason;
}
