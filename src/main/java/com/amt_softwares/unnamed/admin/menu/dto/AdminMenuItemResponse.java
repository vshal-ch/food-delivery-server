package com.amt_softwares.unnamed.admin.menu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AdminMenuItemResponse {
    private UUID id;
    private UUID placeId;
    private UUID categoryId;
    private String name;
    private String description;
    private UUID mediaId;
    private BigDecimal price;
    private boolean veg;
    private boolean available;
    private String flaggedStatus;
    private String flaggedReason;
    private UUID flaggedBy;
}
