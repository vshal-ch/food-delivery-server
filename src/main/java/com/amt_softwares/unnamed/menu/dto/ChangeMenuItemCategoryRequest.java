package com.amt_softwares.unnamed.menu.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChangeMenuItemCategoryRequest {
    private UUID categoryId;
}
