package com.amt_softwares.unnamed.menu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MenuItemResponse {
    private UUID id;
    private UUID categoryId;
    private String name;
    private String description;
    private UUID mediaId;
    private BigDecimal price;
    private boolean veg;
    private boolean available;
    private boolean flagged;
    private String flaggedReason;
    private UUID flaggedBy;
}
