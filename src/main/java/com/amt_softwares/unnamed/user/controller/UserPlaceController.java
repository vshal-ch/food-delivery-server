package com.amt_softwares.unnamed.user.controller;

import com.amt_softwares.unnamed.user.dto.UserMenuItemResponse;
import com.amt_softwares.unnamed.user.dto.UserPlaceDetailResponse;
import com.amt_softwares.unnamed.user.dto.UserPlaceListResponse;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.service.UserPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserPlaceController {

    private final UserPlaceService userPlaceService;

    @GetMapping("/places")
    public ResponseEntity<Page<UserPlaceListResponse>> getPlaces(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userPlaceService.getPlaces(user.getId(), getRole(authentication), pageable));
    }

    @GetMapping("/places/{placeId}")
    public ResponseEntity<UserPlaceDetailResponse> getPlaceWithMenu(
            @PathVariable UUID placeId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userPlaceService.getPlaceWithMenu(user.getId(), getRole(authentication), placeId));
    }

    @GetMapping("/menu/{itemId}")
    public ResponseEntity<UserMenuItemResponse> getMenuItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userPlaceService.getMenuItem(user.getId(), getRole(authentication), itemId));
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority();
    }
}
