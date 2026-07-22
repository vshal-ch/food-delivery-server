package com.amt_softwares.unnamed.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class UserMenuItemResponse {
    private UUID id;
    private UUID placeId;
    private UUID categoryId;
    private String name;
    private String description;
    private UUID mediaId;
    private BigDecimal price;
    private boolean veg;
    private boolean available;
}
