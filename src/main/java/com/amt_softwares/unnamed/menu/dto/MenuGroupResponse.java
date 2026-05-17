package com.amt_softwares.unnamed.menu.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuGroupResponse {
    private MenuCategoryResponse category;
    private List<MenuItemResponse> items;
}
