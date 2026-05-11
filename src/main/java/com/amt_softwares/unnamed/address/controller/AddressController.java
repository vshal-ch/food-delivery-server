package com.amt_softwares.unnamed.address.controller;

import com.amt_softwares.unnamed.address.dto.AddressResponse;
import com.amt_softwares.unnamed.address.dto.CreateAddressRequest;
import com.amt_softwares.unnamed.address.dto.UpdateAddressRequest;
import com.amt_softwares.unnamed.address.service.AddressService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(
            @Valid @RequestBody CreateAddressRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        AddressResponse response = addressService.addAddress(user.getId(), role, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        return ResponseEntity.ok(addressService.updateAddress(user.getId(), role, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        addressService.deleteAddress(user.getId(), role, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable UUID addressId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        return ResponseEntity.ok(addressService.setDefaultAddress(user.getId(), role, addressId));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = getRole(authentication);
        return ResponseEntity.ok(addressService.getAddresses(user.getId(), role));
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority();
    }
}
