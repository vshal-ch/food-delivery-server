package com.amt_softwares.unnamed.user.dto;

import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserMenuGroupResponse {
    private MenuCategoryResponse category;
    private List<UserMenuItemResponse> items;
}
