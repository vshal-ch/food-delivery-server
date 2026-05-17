package com.amt_softwares.unnamed.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMenuCategoryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;
}
