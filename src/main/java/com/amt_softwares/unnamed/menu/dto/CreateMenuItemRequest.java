package com.amt_softwares.unnamed.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateMenuItemRequest {

    private UUID categoryId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private UUID mediaId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @NotNull
    private Boolean veg;

    private Boolean available;
}
