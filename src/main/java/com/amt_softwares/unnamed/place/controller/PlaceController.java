package com.amt_softwares.unnamed.place.controller;

import com.amt_softwares.unnamed.place.dto.CreatePlaceRequest;
import com.amt_softwares.unnamed.place.dto.PlaceResponse;
import com.amt_softwares.unnamed.place.service.PlaceService;
import com.amt_softwares.unnamed.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/places")
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
}
