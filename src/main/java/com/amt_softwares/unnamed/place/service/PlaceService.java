package com.amt_softwares.unnamed.place.service;

import com.amt_softwares.unnamed.place.dto.CreatePlaceRequest;
import com.amt_softwares.unnamed.place.dto.PlaceResponse;
import com.amt_softwares.unnamed.place.entity.Place;
import com.amt_softwares.unnamed.place.repository.PlaceRepository;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final String PLACE_ROLE = "place";
    private static final String APPROVAL_STATUS_PENDING = "pending";

    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public PlaceResponse createPlace(UUID userId, String authenticatedRole, CreatePlaceRequest request) {
        if (!PLACE_ROLE.equals(authenticatedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only place users can create places");
        }

        if (!userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have place role");
        }

        if (placeRepository.existsByOwnerId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Place already exists for this user");
        }

        LocalDateTime now = LocalDateTime.now();
        Place place = Place.builder()
                .id(UUID.randomUUID())
                .ownerId(userId)
                .displayName(cleanRequired(request.getDisplayName()))
                .description(cleanOptional(request.getDescription()))
                .phone(cleanRequired(request.getPhone()))
                .pureVeg(request.getPureVeg())
                .addressLine1(cleanRequired(request.getAddressLine1()))
                .addressLine2(cleanOptional(request.getAddressLine2()))
                .city(cleanRequired(request.getCity()))
                .state(cleanRequired(request.getState()))
                .pincode(cleanRequired(request.getPincode()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .open(false)
                .hidden(true)
                .approvalStatus(APPROVAL_STATUS_PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(placeRepository.save(place));
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

    private PlaceResponse toResponse(Place place) {
        return PlaceResponse.builder()
                .id(place.getId())
                .ownerId(place.getOwnerId())
                .displayName(place.getDisplayName())
                .description(place.getDescription())
                .phone(place.getPhone())
                .pureVeg(place.isPureVeg())
                .addressLine1(place.getAddressLine1())
                .addressLine2(place.getAddressLine2())
                .city(place.getCity())
                .state(place.getState())
                .pincode(place.getPincode())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .open(place.isOpen())
                .hidden(place.isHidden())
                .approvalStatus(place.getApprovalStatus())
                .build();
    }
}
