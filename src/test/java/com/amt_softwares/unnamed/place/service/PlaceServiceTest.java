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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    private static final String PLACE_ROLE = "place";

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceChangeRepository placeChangeRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private PlaceService placeService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(placeRepository, placeChangeRepository, userRoleRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void createPlaceSavesPendingHiddenPlaceForOwner() {
        CreatePlaceRequest request = createPlaceRequest();

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.existsByOwnerId(userId)).thenReturn(false);
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceResponse response = placeService.createPlace(userId, PLACE_ROLE, request);

        ArgumentCaptor<Place> placeCaptor = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(placeCaptor.capture());
        Place savedPlace = placeCaptor.getValue();

        assertThat(savedPlace.getDisplayName()).isEqualTo("Spice House");
        assertThat(savedPlace.getDescription()).isNull();
        assertThat(savedPlace.isHidden()).isTrue();
        assertThat(savedPlace.isOpen()).isFalse();
        assertThat(savedPlace.getApprovalStatus()).isEqualTo("pending");
        assertThat(response.getApprovalStatus()).isEqualTo("pending");
    }

    @Test
    void createPlaceRejectsSecondPlaceForOwner() {
        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.existsByOwnerId(userId)).thenReturn(true);

        assertThatThrownBy(() -> placeService.createPlace(userId, PLACE_ROLE, createPlaceRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void getPlaceReturnsApprovedAndPendingChange() {
        Place approvedPlace = approvedPlace(userId);
        PlaceChange placeChange = rejectedChange(approvedPlace.getId(), userId);
        placeChange.setStatus("pending");
        placeChange.setRejectionReason(null);

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.of(approvedPlace));
        when(placeChangeRepository.findFirstByPlaceIdAndOwnerIdAndStatusInOrderByCreatedAtDesc(
                approvedPlace.getId(),
                userId,
                List.of("pending", "rejected")
        )).thenReturn(Optional.of(placeChange));

        PlaceStateResponse response = placeService.getPlace(userId, PLACE_ROLE);

        assertThat(response.getApprovedDetails()).isNotNull();
        assertThat(response.getApprovedDetails().getId()).isEqualTo(approvedPlace.getId());
        assertThat(response.getUnapprovedDetails()).isNotNull();
        assertThat(response.getUnapprovedDetails().getChangeId()).isEqualTo(placeChange.getId());
    }

    @Test
    void updatePlaceUpdatesInitialSubmissionAndResetsReviewStateWhenPlaceRowDoesNotExist() {
        UpdatePlaceRequest request = updatePlaceRequest();
        PlaceChange initialSubmission = rejectedChange(null, userId);

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.empty());
        when(placeChangeRepository.findFirstByOwnerIdAndPlaceIdIsNullAndStatusInOrderByCreatedAtDesc(
                userId,
                List.of("pending", "rejected")
        )).thenReturn(Optional.of(initialSubmission));
        when(placeChangeRepository.save(any(PlaceChange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceStateResponse response = placeService.updatePlace(userId, PLACE_ROLE, request);

        assertThat(initialSubmission.getDisplayName()).isEqualTo("Updated Spice House");
        assertThat(initialSubmission.getDescription()).isNull();
        assertThat(initialSubmission.getStatus()).isEqualTo("pending");
        assertThat(initialSubmission.getRejectionReason()).isNull();
        assertThat(initialSubmission.getReviewedBy()).isNull();
        assertThat(response.getApprovedDetails()).isNull();
        assertThat(response.getUnapprovedDetails().getApprovalStatus()).isEqualTo("pending");
    }

    @Test
    void updatePlaceUpdatesPendingPlaceInPlacesTable() {
        UpdatePlaceRequest request = updatePlaceRequest();
        Place pendingPlace = pendingPlace(userId);
        pendingPlace.setReviewComments("Fix docs");
        pendingPlace.setReviewedBy(UUID.randomUUID());
        pendingPlace.setReviewedAt(LocalDateTime.now().minusHours(1));

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.of(pendingPlace));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceStateResponse response = placeService.updatePlace(userId, PLACE_ROLE, request);

        assertThat(pendingPlace.getDisplayName()).isEqualTo("Updated Spice House");
        assertThat(pendingPlace.getApprovalStatus()).isEqualTo("pending");
        assertThat(pendingPlace.getReviewComments()).isNull();
        assertThat(pendingPlace.getReviewedBy()).isNull();
        assertThat(pendingPlace.getReviewedAt()).isNull();
        assertThat(response.getUnapprovedDetails().getId()).isEqualTo(pendingPlace.getId());
    }

    @Test
    void updatePlaceCreatesNewChangeForApprovedPlaceWhenNoneExists() {
        UpdatePlaceRequest request = updatePlaceRequest();
        Place approvedPlace = approvedPlace(userId);

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.of(approvedPlace));
        when(placeChangeRepository.findFirstByPlaceIdAndOwnerIdAndStatusInOrderByCreatedAtDesc(
                approvedPlace.getId(),
                userId,
                List.of("pending", "rejected")
        )).thenReturn(Optional.empty());
        when(placeChangeRepository.save(any(PlaceChange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceStateResponse response = placeService.updatePlace(userId, PLACE_ROLE, request);

        ArgumentCaptor<PlaceChange> changeCaptor = ArgumentCaptor.forClass(PlaceChange.class);
        verify(placeChangeRepository).save(changeCaptor.capture());
        PlaceChange savedChange = changeCaptor.getValue();

        assertThat(savedChange.getPlaceId()).isEqualTo(approvedPlace.getId());
        assertThat(savedChange.getOwnerId()).isEqualTo(userId);
        assertThat(savedChange.getStatus()).isEqualTo("pending");
        assertThat(response.getApprovedDetails().getId()).isEqualTo(approvedPlace.getId());
        assertThat(response.getUnapprovedDetails().getChangeId()).isEqualTo(savedChange.getId());
    }

    @Test
    void setOpenStatusRejectsUnapprovedPlace() {
        Place pendingPlace = pendingPlace(userId);
        SetPlaceOpenRequest request = new SetPlaceOpenRequest();
        request.setOpen(true);

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.of(pendingPlace));

        assertThatThrownBy(() -> placeService.setOpenStatus(userId, PLACE_ROLE, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void setOpenStatusUpdatesApprovedPlace() {
        Place approvedPlace = approvedPlace(userId);
        SetPlaceOpenRequest request = new SetPlaceOpenRequest();
        request.setOpen(true);

        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.of(approvedPlace));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceResponse response = placeService.setOpenStatus(userId, PLACE_ROLE, request);

        assertThat(response.isOpen()).isTrue();
    }

    private CreatePlaceRequest createPlaceRequest() {
        CreatePlaceRequest request = new CreatePlaceRequest();
        request.setDisplayName("  Spice House  ");
        request.setDescription("  ");
        request.setPhone("  9999999999  ");
        request.setPureVeg(true);
        request.setAddressLine1("  MG Road  ");
        request.setAddressLine2(" ");
        request.setCity("  Bengaluru  ");
        request.setState("  Karnataka  ");
        request.setPincode("  560001  ");
        request.setLatitude(new BigDecimal("12.9716"));
        request.setLongitude(new BigDecimal("77.5946"));
        return request;
    }

    private UpdatePlaceRequest updatePlaceRequest() {
        UpdatePlaceRequest request = new UpdatePlaceRequest();
        request.setDisplayName("  Updated Spice House  ");
        request.setDescription("  ");
        request.setPhone("  8888888888  ");
        request.setPureVeg(false);
        request.setAddressLine1("  Residency Road  ");
        request.setAddressLine2(" ");
        request.setCity("  Mumbai  ");
        request.setState("  Maharashtra  ");
        request.setPincode("  400001  ");
        request.setLatitude(new BigDecimal("19.0760"));
        request.setLongitude(new BigDecimal("72.8777"));
        return request;
    }

    private Place approvedPlace(UUID ownerId) {
        return Place.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .displayName("Approved Place")
                .description("Desc")
                .phone("9999999999")
                .pureVeg(true)
                .addressLine1("Line 1")
                .addressLine2("Line 2")
                .city("City")
                .state("State")
                .pincode("123456")
                .latitude(new BigDecimal("10.0000"))
                .longitude(new BigDecimal("20.0000"))
                .open(false)
                .hidden(false)
                .approvalStatus("approved")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private Place pendingPlace(UUID ownerId) {
        return Place.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .displayName("Pending Place")
                .description("Desc")
                .phone("9999999999")
                .pureVeg(true)
                .addressLine1("Line 1")
                .addressLine2("Line 2")
                .city("City")
                .state("State")
                .pincode("123456")
                .latitude(new BigDecimal("10.0000"))
                .longitude(new BigDecimal("20.0000"))
                .open(false)
                .hidden(true)
                .approvalStatus("pending")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private PlaceChange rejectedChange(UUID placeId, UUID ownerId) {
        return PlaceChange.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .ownerId(ownerId)
                .displayName("Rejected Place")
                .description("Desc")
                .phone("9999999999")
                .pureVeg(true)
                .addressLine1("Line 1")
                .addressLine2("Line 2")
                .city("City")
                .state("State")
                .pincode("123456")
                .latitude(new BigDecimal("10.0000"))
                .longitude(new BigDecimal("20.0000"))
                .status("rejected")
                .rejectionReason("Needs changes")
                .reviewedBy(UUID.randomUUID())
                .reviewedAt(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
