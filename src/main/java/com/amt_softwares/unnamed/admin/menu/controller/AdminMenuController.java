package com.amt_softwares.unnamed.admin.menu.controller;

import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import com.amt_softwares.unnamed.admin.menu.dto.AdminMenuItemResponse;
import com.amt_softwares.unnamed.admin.menu.dto.FlagMenuItemRequest;
import com.amt_softwares.unnamed.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/admin/menu")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuService menuService;

    @GetMapping("/flagged_items")
    public ResponseEntity<List<AdminMenuItemResponse>> getFlaggedItems() {
        return ResponseEntity.ok(menuService.getFlaggedItems());
    }

    @GetMapping("/submitted_for_review")
    public ResponseEntity<List<AdminMenuItemResponse>> getSubmittedForReviewItems() {
        return ResponseEntity.ok(menuService.getSubmittedForReviewItems());
    }

    @PatchMapping("/{itemId}/flag")
    public ResponseEntity<AdminMenuItemResponse> flagMenuItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody FlagMenuItemRequest request,
            Authentication authentication
    ) {
        Admin admin = (Admin) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.flagMenuItemForAdmin(itemId, admin.getId(), request));
    }
}
