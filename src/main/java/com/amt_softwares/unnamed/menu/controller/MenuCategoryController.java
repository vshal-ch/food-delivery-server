package com.amt_softwares.unnamed.menu.controller;

import com.amt_softwares.unnamed.menu.dto.CreateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.service.MenuService;
import com.amt_softwares.unnamed.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/owner/menu-categories")
@RequiredArgsConstructor
public class MenuCategoryController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuCategoryResponse> createCategory(
            @Valid @RequestBody CreateMenuCategoryRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.createCategory(user.getId(), getRole(authentication), request));
    }

    @GetMapping
    public ResponseEntity<List<MenuCategoryResponse>> getCategories(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.getCategories(user.getId(), getRole(authentication)));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<MenuCategoryResponse> getCategory(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.getCategory(user.getId(), getRole(authentication), categoryId));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<MenuCategoryResponse> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateMenuCategoryRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.updateCategory(user.getId(), getRole(authentication), categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        menuService.deleteCategory(user.getId(), getRole(authentication), categoryId);
        return ResponseEntity.ok().build();
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority();
    }
}
