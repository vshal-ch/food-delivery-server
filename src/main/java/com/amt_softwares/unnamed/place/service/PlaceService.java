package com.amt_softwares.unnamed.place.service;

import com.amt_softwares.unnamed.place.dto.CreatePlaceRequest;
import com.amt_softwares.unnamed.place.dto.PlaceResponse;
import com.amt_softwares.unnamed.place.dto.PlaceStateResponse;
import com.amt_softwares.unnamed.place.dto.SetPlaceOpenRequest;
import com.amt_softwares.unnamed.place.dto.UpdatePlaceRequest;
import com.amt_softwares.unnamed.place.entity.Place;
import com.amt_softwares.unnamed.place.entity.PlaceChange;
import com.amt_softwares.unnamed.place.repository.PlaceChangeRepository;
import com.amt_softwares.unnamed.place.repository.PlaceRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final String PLACE_ROLE = "place";
    private static final String CHANGE_STATUS_REJECTED = "rejected";
    private static final String CHANGE_STATUS_PENDING = "pending";
    private static final List<String> ACTIVE_CHANGE_STATUSES = List.of(CHANGE_STATUS_PENDING, CHANGE_STATUS_REJECTED);

    private final PlaceRepository placeRepository;
    private final PlaceChangeRepository placeChangeRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public PlaceResponse createPlace(UUID userId, String authenticatedRole, CreatePlaceRequest request) {
        verifyPlaceRole(userId, authenticatedRole);

        if (placeRepository.existsByOwnerId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Place already exists for this user");
        }

        if (placeChangeRepository.existsByOwnerIdAndPlaceIdIsNullAndStatusIn(userId, ACTIVE_CHANGE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Place submission already exists for this user");
        }

        PlaceChange placeChange = PlaceChange.builder()
                .id(UUID.randomUUID())
                .placeId(null)
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
                .status(CHANGE_STATUS_PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(placeChangeRepository.save(placeChange));
    }

    @Transactional(readOnly = true)
    public PlaceStateResponse getPlace(UUID userId, String authenticatedRole) {
        verifyPlaceRole(userId, authenticatedRole);

        Optional<Place> place = placeRepository.findByOwnerId(userId);
        if (place.isEmpty()) {
            PlaceChange initialSubmission = getInitialSubmission(userId);
            return PlaceStateResponse.builder()
                    .approvedDetails(null)
                    .unapprovedDetails(toResponse(initialSubmission))
                    .build();
        }

        Place approvedPlace = place.get();
        PlaceResponse unapprovedDetails = placeChangeRepository
                .findFirstByPlaceIdAndOwnerIdAndStatusInOrderByCreatedAtDesc(
                        approvedPlace.getId(),
                        userId,
                        ACTIVE_CHANGE_STATUSES
                )
                .map(this::toResponse)
                .orElse(null);

        return PlaceStateResponse.builder()
                .approvedDetails(toResponse(approvedPlace))
                .unapprovedDetails(unapprovedDetails)
                .build();
    }

    @Transactional
    public PlaceStateResponse updatePlace(UUID userId, String authenticatedRole, UpdatePlaceRequest request) {
        verifyPlaceRole(userId, authenticatedRole);

        Optional<Place> place = placeRepository.findByOwnerId(userId);
        if (place.isEmpty()) {
            PlaceChange initialSubmission = getInitialSubmission(userId);
            applyRequest(initialSubmission, request);
            markPending(initialSubmission);
            PlaceChange savedChange = placeChangeRepository.save(initialSubmission);

            return PlaceStateResponse.builder()
                    .approvedDetails(null)
                    .unapprovedDetails(toResponse(savedChange))
                    .build();
        }

        Place approvedPlace = place.get();
        PlaceChange placeChange = placeChangeRepository
                .findFirstByPlaceIdAndOwnerIdAndStatusInOrderByCreatedAtDesc(
                        approvedPlace.getId(),
                        userId,
                        ACTIVE_CHANGE_STATUSES
                )
                .orElseGet(() -> PlaceChange.builder()
                        .id(UUID.randomUUID())
                        .placeId(approvedPlace.getId())
                        .ownerId(userId)
                        .createdAt(LocalDateTime.now())
                        .build());

        applyRequest(placeChange, request);
        markPending(placeChange);
        PlaceChange savedChange = placeChangeRepository.save(placeChange);

        return PlaceStateResponse.builder()
                .approvedDetails(toResponse(approvedPlace))
                .unapprovedDetails(toResponse(savedChange))
                .build();
    }

    @Transactional
    public PlaceResponse setOpenStatus(UUID userId, String authenticatedRole, SetPlaceOpenRequest request) {
        verifyPlaceRole(userId, authenticatedRole);

        Place place = placeRepository.findByOwnerId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved place not found"));

        if (!"approved".equals(place.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Place must be approved before changing open status");
        }

        place.setOpen(request.getOpen());
        place.setUpdatedAt(LocalDateTime.now());

        return toResponse(placeRepository.save(place));
    }

    private void verifyPlaceRole(UUID userId, String authenticatedRole) {
        if (!PLACE_ROLE.equals(authenticatedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only place users can access places");
        }

        if (!userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have place role");
        }
    }

    private PlaceChange getInitialSubmission(UUID userId) {
        return placeChangeRepository
                .findFirstByOwnerIdAndPlaceIdIsNullAndStatusInOrderByCreatedAtDesc(userId, ACTIVE_CHANGE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place submission not found"));
    }

    private void applyRequest(PlaceChange placeChange, UpdatePlaceRequest request) {
        placeChange.setDisplayName(cleanRequired(request.getDisplayName()));
        placeChange.setDescription(cleanOptional(request.getDescription()));
        placeChange.setPhone(cleanRequired(request.getPhone()));
        placeChange.setPureVeg(request.getPureVeg());
        placeChange.setAddressLine1(cleanRequired(request.getAddressLine1()));
        placeChange.setAddressLine2(cleanOptional(request.getAddressLine2()));
        placeChange.setCity(cleanRequired(request.getCity()));
        placeChange.setState(cleanRequired(request.getState()));
        placeChange.setPincode(cleanRequired(request.getPincode()));
        placeChange.setLatitude(request.getLatitude());
        placeChange.setLongitude(request.getLongitude());
    }

    private void markPending(PlaceChange placeChange) {
        placeChange.setStatus(CHANGE_STATUS_PENDING);
        placeChange.setRejectionReason(null);
        placeChange.setReviewedBy(null);
        placeChange.setReviewedAt(null);
        placeChange.setCreatedAt(LocalDateTime.now());
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
                .changeId(null)
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
                .rejectionReason(null)
                .build();
    }

    private PlaceResponse toResponse(PlaceChange placeChange) {
        return PlaceResponse.builder()
                .id(placeChange.getPlaceId())
                .changeId(placeChange.getId())
                .ownerId(placeChange.getOwnerId())
                .displayName(placeChange.getDisplayName())
                .description(placeChange.getDescription())
                .phone(placeChange.getPhone())
                .pureVeg(placeChange.isPureVeg())
                .addressLine1(placeChange.getAddressLine1())
                .addressLine2(placeChange.getAddressLine2())
                .city(placeChange.getCity())
                .state(placeChange.getState())
                .pincode(placeChange.getPincode())
                .latitude(placeChange.getLatitude())
                .longitude(placeChange.getLongitude())
                .open(false)
                .hidden(true)
                .approvalStatus(placeChange.getStatus())
                .rejectionReason(placeChange.getRejectionReason())
                .build();
    }
}
