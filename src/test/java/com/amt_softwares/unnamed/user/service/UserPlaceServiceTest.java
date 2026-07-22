package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.menu.entity.MenuCategory;
import com.amt_softwares.unnamed.menu.entity.MenuItem;
import com.amt_softwares.unnamed.menu.entity.MenuItemFlagStatus;
import com.amt_softwares.unnamed.menu.repository.MenuCategoryRepository;
import com.amt_softwares.unnamed.menu.repository.MenuItemRepository;
import com.amt_softwares.unnamed.place.entity.Place;
import com.amt_softwares.unnamed.place.repository.PlaceRepository;
import com.amt_softwares.unnamed.user.dto.UserMenuItemResponse;
import com.amt_softwares.unnamed.user.dto.UserPlaceDetailResponse;
import com.amt_softwares.unnamed.user.dto.UserPlaceListResponse;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPlaceServiceTest {

    private static final String USER_ROLE = "user";

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private UserPlaceService userPlaceService;
    private UUID userId;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        userPlaceService = new UserPlaceService(
                placeRepository,
                menuCategoryRepository,
                menuItemRepository,
                userRoleRepository
        );
        userId = UUID.randomUUID();
        placeId = UUID.randomUUID();
    }

    @Test
    void getPlacesReturnsPagedVisiblePlacesAndMapsListFields() {
        Place place = approvedVisiblePlace();
        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(placeRepository.findByApprovalStatusAndHiddenFalseOrderByCreatedAtDesc("approved", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(place), PageRequest.of(0, 10), 1));

        Page<UserPlaceListResponse> page = userPlaceService.getPlaces(userId, USER_ROLE, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        UserPlaceListResponse response = page.getContent().getFirst();
        assertThat(response.getDisplayName()).isEqualTo("Visible Place");
    }

    @Test
    void getPlaceWithMenuReturnsGroupedVisibleMenu() {
        MenuCategory category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .name("Starters")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        MenuItem categorized = menuItem(UUID.randomUUID(), category.getId());
        MenuItem uncategorized = menuItem(UUID.randomUUID(), null);

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(placeRepository.findByIdAndApprovalStatusAndHiddenFalse(placeId, "approved"))
                .thenReturn(Optional.of(approvedVisiblePlace()));
        when(menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId)).thenReturn(List.of(category));
        when(menuItemRepository.findByPlaceIdAndAvailableTrueAndFlaggedStatusOrderByCreatedAtAsc(
                placeId,
                MenuItemFlagStatus.no_flag
        )).thenReturn(List.of(categorized, uncategorized));

        UserPlaceDetailResponse response = userPlaceService.getPlaceWithMenu(userId, USER_ROLE, placeId);

        assertThat(response.getMenu()).hasSize(2);
        assertThat(response.getMenu().get(0).getCategory().getName()).isEqualTo("Starters");
        assertThat(response.getMenu().get(0).getItems()).hasSize(1);
        assertThat(response.getMenu().get(1).getCategory()).isNull();
        assertThat(response.getMenu().get(1).getItems()).hasSize(1);
    }

    @Test
    void getMenuItemRejectsItemWhenPlaceIsNotVisible() {
        UUID itemId = UUID.randomUUID();
        MenuItem item = menuItem(itemId, null);
        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(menuItemRepository.findByIdAndAvailableTrueAndFlaggedStatus(itemId, MenuItemFlagStatus.no_flag))
                .thenReturn(Optional.of(item));
        when(placeRepository.findByIdAndApprovalStatusAndHiddenFalse(placeId, "approved"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPlaceService.getMenuItem(userId, USER_ROLE, itemId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMenuItemReturnsVisibleItem() {
        UUID itemId = UUID.randomUUID();
        MenuItem item = menuItem(itemId, null);
        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(menuItemRepository.findByIdAndAvailableTrueAndFlaggedStatus(itemId, MenuItemFlagStatus.no_flag))
                .thenReturn(Optional.of(item));
        when(placeRepository.findByIdAndApprovalStatusAndHiddenFalse(placeId, "approved"))
                .thenReturn(Optional.of(approvedVisiblePlace()));

        UserMenuItemResponse response = userPlaceService.getMenuItem(userId, USER_ROLE, itemId);

        assertThat(response.getId()).isEqualTo(itemId);
        assertThat(response.getPlaceId()).isEqualTo(placeId);
    }

    @Test
    void userPlaceEndpointsRejectWrongRole() {
        assertThatThrownBy(() -> userPlaceService.getPlaces(userId, "place", PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Place approvedVisiblePlace() {
        return Place.builder()
                .id(placeId)
                .displayName("Visible Place")
                .description("Desc")
                .phone("9999999999")
                .pureVeg(true)
                .addressLine1("Line 1")
                .addressLine2("Line 2")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .open(true)
                .hidden(false)
                .approvalStatus("approved")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private MenuItem menuItem(UUID id, UUID categoryId) {
        return MenuItem.builder()
                .id(id)
                .placeId(placeId)
                .categoryId(categoryId)
                .name("Item")
                .description("Desc")
                .price(new BigDecimal("120.00"))
                .veg(true)
                .available(true)
                .flaggedStatus(MenuItemFlagStatus.no_flag)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
