package com.amt_softwares.unnamed.admin.menu.dto;

import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminMenuGroupResponse {
    private MenuCategoryResponse category;
    private List<AdminMenuItemResponse> items;
}
