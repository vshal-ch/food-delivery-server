package com.amt_softwares.unnamed.place.controller;

import com.amt_softwares.unnamed.place.dto.CreatePlaceRequest;
import com.amt_softwares.unnamed.place.dto.PlaceResponse;
import com.amt_softwares.unnamed.place.dto.PlaceStateResponse;
import com.amt_softwares.unnamed.place.dto.SetPlaceOpenRequest;
import com.amt_softwares.unnamed.place.dto.UpdatePlaceRequest;
import com.amt_softwares.unnamed.place.service.PlaceService;
import com.amt_softwares.unnamed.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/owner/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    public ResponseEntity<PlaceResponse> createPlace(
            @Valid @RequestBody CreatePlaceRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        PlaceResponse response = placeService.createPlace(user.getId(), role, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PlaceStateResponse> getPlace(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(placeService.getPlace(user.getId(), role));
    }

    @PutMapping
    public ResponseEntity<PlaceStateResponse> updatePlace(
            @Valid @RequestBody UpdatePlaceRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(placeService.updatePlace(user.getId(), role, request));
    }

    @PatchMapping("/open-status")
    public ResponseEntity<PlaceResponse> setOpenStatus(
            @Valid @RequestBody SetPlaceOpenRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(placeService.setOpenStatus(user.getId(), role, request));
    }
}
