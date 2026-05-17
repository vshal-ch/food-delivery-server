package com.amt_softwares.unnamed.admin.places.controller;

import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import com.amt_softwares.unnamed.admin.menu.dto.AdminMenuGroupResponse;
import com.amt_softwares.unnamed.admin.places.dto.AdminPlaceResponse;
import com.amt_softwares.unnamed.admin.places.dto.AdminPlaceReviewRequest;
import com.amt_softwares.unnamed.admin.places.service.AdminPlaceService;
import com.amt_softwares.unnamed.menu.service.MenuService;
import com.amt_softwares.unnamed.place.dto.SetPlaceOpenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/admin/places")
@RequiredArgsConstructor
public class AdminPlaceController {

    private final AdminPlaceService adminPlaceService;
    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<List<AdminPlaceResponse>> getPlaces() {
        return ResponseEntity.ok(adminPlaceService.getPlaces());
    }

    @GetMapping("/review")
    public ResponseEntity<List<AdminPlaceResponse>> getPlacesSubmittedForReview() {
        return ResponseEntity.ok(adminPlaceService.getPlacesSubmittedForReview());
    }

    @GetMapping("/review/{changeId}")
    public ResponseEntity<AdminPlaceResponse> getPlaceSubmittedForReview(
            @PathVariable UUID changeId
    ) {
        return ResponseEntity.ok(adminPlaceService.getPlaceSubmittedForReview(changeId));
    }

    @PostMapping("/review/{changeId}")
    public ResponseEntity<AdminPlaceResponse> submitReview(
            @PathVariable UUID changeId,
            @Valid @RequestBody AdminPlaceReviewRequest request,
            Authentication authentication
    ) {
        Admin admin = (Admin) authentication.getPrincipal();
        return ResponseEntity.ok(adminPlaceService.submitReview(changeId, admin, request));
    }

    @PatchMapping("/{placeId}/hide")
    public ResponseEntity<AdminPlaceResponse> hidePlace(@PathVariable UUID placeId) {
        return ResponseEntity.ok(adminPlaceService.hidePlace(placeId));
    }

    @PatchMapping("/{placeId}/unhide")
    public ResponseEntity<AdminPlaceResponse> unhidePlace(@PathVariable UUID placeId) {
        return ResponseEntity.ok(adminPlaceService.unhidePlace(placeId));
    }

    @PatchMapping("/{placeId}/open-status")
    public ResponseEntity<AdminPlaceResponse> setOpenStatus(
            @PathVariable UUID placeId,
            @Valid @RequestBody SetPlaceOpenRequest request
    ) {
        return ResponseEntity.ok(adminPlaceService.setOpenStatus(placeId, request));
    }

    @GetMapping("/{placeId}/menu")
    public ResponseEntity<List<AdminMenuGroupResponse>> getPlaceMenu(@PathVariable UUID placeId) {
        return ResponseEntity.ok(menuService.getGroupedMenuForAdmin(placeId));
    }
}
