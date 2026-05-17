package com.amt_softwares.unnamed.menu.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MenuCategoryResponse {
    private UUID id;
    private String name;
}
