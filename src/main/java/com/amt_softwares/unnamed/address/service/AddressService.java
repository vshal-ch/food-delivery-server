package com.amt_softwares.unnamed.address.service;

import com.amt_softwares.unnamed.address.dto.AddressResponse;
import com.amt_softwares.unnamed.address.dto.CreateAddressRequest;
import com.amt_softwares.unnamed.address.dto.UpdateAddressRequest;
import com.amt_softwares.unnamed.address.entity.Address;
import com.amt_softwares.unnamed.address.repository.AddressRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final String USER_ROLE = "user";

    private final AddressRepository addressRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public AddressResponse addAddress(UUID userId, String authenticatedRole, CreateAddressRequest request) {
        verifyUserRole(userId, authenticatedRole);

        LocalDateTime now = LocalDateTime.now();
        boolean firstAddress = !addressRepository.existsByUserId(userId);
        Address address = Address.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .label(cleanOptional(request.getLabel()))
                .addressLine1(cleanRequired(request.getAddressLine1()))
                .addressLine2(cleanOptional(request.getAddressLine2()))
                .city(cleanRequired(request.getCity()))
                .state(cleanRequired(request.getState()))
                .pincode(cleanRequired(request.getPincode()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .defaultAddress(firstAddress)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, String authenticatedRole, UUID addressId, UpdateAddressRequest request) {
        verifyUserRole(userId, authenticatedRole);

        Address address = getOwnedAddress(userId, addressId);
        address.setLabel(cleanOptional(request.getLabel()));
        address.setAddressLine1(cleanRequired(request.getAddressLine1()));
        address.setAddressLine2(cleanOptional(request.getAddressLine2()));
        address.setCity(cleanRequired(request.getCity()));
        address.setState(cleanRequired(request.getState()));
        address.setPincode(cleanRequired(request.getPincode()));
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setUpdatedAt(LocalDateTime.now());

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, String authenticatedRole, UUID addressId) {
        verifyUserRole(userId, authenticatedRole);

        Address address = getOwnedAddress(userId, addressId);
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, String authenticatedRole, UUID addressId) {
        verifyUserRole(userId, authenticatedRole);

        Address address = getOwnedAddress(userId, addressId);
        addressRepository.clearDefaultForUser(userId);
        address.setDefaultAddress(true);
        address.setUpdatedAt(LocalDateTime.now());

        return toResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID userId, String authenticatedRole) {
        verifyUserRole(userId, authenticatedRole);

        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void verifyUserRole(UUID userId, String authenticatedRole) {
        if (!USER_ROLE.equals(authenticatedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only user role can access addresses");
        }

        if (!userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have user role");
        }
    }

    private Address getOwnedAddress(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    private String cleanRequired(String value) {
        return value.trim();
    }

    private String cleanOptional(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .defaultAddress(address.isDefaultAddress())
                .build();
    }
}
