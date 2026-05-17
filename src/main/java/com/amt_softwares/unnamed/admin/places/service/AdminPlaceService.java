package com.amt_softwares.unnamed.admin.places.service;

import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import com.amt_softwares.unnamed.admin.places.dto.AdminPlaceResponse;
import com.amt_softwares.unnamed.admin.places.dto.AdminPlaceReviewRequest;
import com.amt_softwares.unnamed.place.entity.Place;
import com.amt_softwares.unnamed.place.entity.PlaceChange;
import com.amt_softwares.unnamed.place.dto.SetPlaceOpenRequest;
import com.amt_softwares.unnamed.place.repository.PlaceChangeRepository;
import com.amt_softwares.unnamed.place.repository.PlaceRepository;
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
public class AdminPlaceService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final List<String> ACTIVE_CHANGE_STATUSES = List.of(STATUS_PENDING, STATUS_REJECTED);


    private final PlaceRepository placeRepository;
    private final PlaceChangeRepository placeChangeRepository;

    @Transactional(readOnly = true)
    public List<AdminPlaceResponse> getPlaces() {
        return placeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPlaceResponse> getPlacesSubmittedForReview() {
        return placeChangeRepository.findByStatusInOrderByCreatedAtDesc(ACTIVE_CHANGE_STATUSES)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminPlaceResponse submitReview(UUID changeId, Admin admin, AdminPlaceReviewRequest request) {
        PlaceChange placeChange = placeChangeRepository.findByIdAndStatusIn(changeId, ACTIVE_CHANGE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending place submission not found"));

        String status = cleanRequired(request.getApprovalStatus()).toLowerCase();
        if (STATUS_APPROVED.equals(status)) {
            return approve(placeChange, admin, request.getReviewComments());
        }

        if (STATUS_REJECTED.equals(status)) {
            return reject(placeChange, admin, request.getReviewComments());
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "approvalStatus must be approved or rejected");
    }

    @Transactional
    public AdminPlaceResponse hidePlace(UUID placeId) {
        Place place = getPlace(placeId);
        place.setHidden(true);
        place.setUpdatedAt(LocalDateTime.now());
        return toResponse(placeRepository.save(place));
    }

    @Transactional
    public AdminPlaceResponse unhidePlace(UUID placeId) {
        Place place = getPlace(placeId);
        place.setHidden(false);
        place.setUpdatedAt(LocalDateTime.now());
        return toResponse(placeRepository.save(place));
    }

    @Transactional
    public AdminPlaceResponse setOpenStatus(UUID placeId, SetPlaceOpenRequest request) {
        Place place = getPlace(placeId);
        place.setOpen(request.getOpen());
        place.setUpdatedAt(LocalDateTime.now());
        return toResponse(placeRepository.save(place));
    }

    private AdminPlaceResponse approve(PlaceChange placeChange, Admin admin, String reviewComments) {
        LocalDateTime now = LocalDateTime.now();
        Place place = placeChange.getPlaceId() == null
                ? createPlaceFromChange(placeChange, now)
                : updatePlaceFromChange(getPlace(placeChange.getPlaceId()), placeChange, now);

        place.setReviewedBy(admin.getId());
        place.setReviewedAt(now);
        place.setReviewComments(cleanOptional(reviewComments));

        Place savedPlace = placeRepository.save(place);
        placeChangeRepository.delete(placeChange);

        return toResponse(savedPlace);
    }

    private AdminPlaceResponse reject(PlaceChange placeChange, Admin admin, String rejectionReason) {
        String cleanedReason = cleanOptional(rejectionReason);
        if (cleanedReason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rejectionReason is required when rejecting");
        }

        placeChange.setStatus(STATUS_REJECTED);
        placeChange.setRejectionReason(cleanedReason);
        placeChange.setReviewedBy(admin.getId());
        placeChange.setReviewedAt(LocalDateTime.now());
        return toResponse(placeChangeRepository.save(placeChange));
    }

    private Place createPlaceFromChange(PlaceChange placeChange, LocalDateTime now) {
        return Place.builder()
                .id(UUID.randomUUID())
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
                .hidden(false)
                .approvalStatus(STATUS_APPROVED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Place updatePlaceFromChange(Place place, PlaceChange placeChange, LocalDateTime now) {
        place.setDisplayName(placeChange.getDisplayName());
        place.setDescription(placeChange.getDescription());
        place.setPhone(placeChange.getPhone());
        place.setPureVeg(placeChange.isPureVeg());
        place.setAddressLine1(placeChange.getAddressLine1());
        place.setAddressLine2(placeChange.getAddressLine2());
        place.setCity(placeChange.getCity());
        place.setState(placeChange.getState());
        place.setPincode(placeChange.getPincode());
        place.setLatitude(placeChange.getLatitude());
        place.setLongitude(placeChange.getLongitude());
        place.setApprovalStatus(STATUS_APPROVED);
        place.setUpdatedAt(now);
        return place;
    }

    private Place getPlace(UUID placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found"));
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

    private AdminPlaceResponse toResponse(Place place) {
        return AdminPlaceResponse.builder()
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
                .reviewedBy(place.getReviewedBy())
                .reviewedAt(place.getReviewedAt())
                .reviewComments(place.getReviewComments())
                .build();
    }

    private AdminPlaceResponse toResponse(PlaceChange placeChange) {
        return AdminPlaceResponse.builder()
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
                .reviewedBy(placeChange.getReviewedBy())
                .reviewedAt(placeChange.getReviewedAt())
                .reviewComments(null)
                .build();
    }
}
