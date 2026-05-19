package com.amt_softwares.unnamed.menu.controller;

import com.amt_softwares.unnamed.menu.dto.ChangeMenuItemCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.CreateMenuItemRequest;
import com.amt_softwares.unnamed.menu.dto.MenuGroupResponse;
import com.amt_softwares.unnamed.menu.dto.MenuItemResponse;
import com.amt_softwares.unnamed.menu.dto.SetMenuItemAvailabilityRequest;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuItemRequest;
import com.amt_softwares.unnamed.menu.service.MenuService;
import com.amt_softwares.unnamed.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/owner/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.createMenuItem(user.getId(), getRole(authentication), request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.updateMenuItem(user.getId(), getRole(authentication), itemId, request));
    }

    @PatchMapping("/{itemId}/availability")
    public ResponseEntity<MenuItemResponse> setMenuItemAvailability(
            @PathVariable UUID itemId,
            @Valid @RequestBody SetMenuItemAvailabilityRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.setMenuItemAvailability(user.getId(), getRole(authentication), itemId, request));
    }

    @PatchMapping("/{itemId}/category")
    public ResponseEntity<MenuItemResponse> changeMenuItemCategory(
            @PathVariable UUID itemId,
            @RequestBody ChangeMenuItemCategoryRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.changeMenuItemCategory(
                user.getId(),
                getRole(authentication),
                itemId,
                request.getCategoryId()
        ));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<MenuItemResponse> getMenuItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.getMenuItem(user.getId(), getRole(authentication), itemId));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        menuService.deleteMenuItem(user.getId(), getRole(authentication), itemId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<MenuGroupResponse>> getGroupedMenu(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.getGroupedMenu(user.getId(), getRole(authentication)));
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority();
    }
}
