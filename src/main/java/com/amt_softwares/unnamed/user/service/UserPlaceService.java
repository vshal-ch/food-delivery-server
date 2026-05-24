package com.amt_softwares.unnamed.user.service;

import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import com.amt_softwares.unnamed.menu.entity.MenuCategory;
import com.amt_softwares.unnamed.menu.entity.MenuItem;
import com.amt_softwares.unnamed.menu.entity.MenuItemFlagStatus;
import com.amt_softwares.unnamed.menu.repository.MenuCategoryRepository;
import com.amt_softwares.unnamed.menu.repository.MenuItemRepository;
import com.amt_softwares.unnamed.place.entity.Place;
import com.amt_softwares.unnamed.place.repository.PlaceRepository;
import com.amt_softwares.unnamed.user.dto.UserMenuGroupResponse;
import com.amt_softwares.unnamed.user.dto.UserMenuItemResponse;
import com.amt_softwares.unnamed.user.dto.UserPlaceDetailResponse;
import com.amt_softwares.unnamed.user.dto.UserPlaceListResponse;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPlaceService {

    private static final String USER_ROLE = "user";
    private static final String STATUS_APPROVED = "approved";
    private static final MenuItemFlagStatus VISIBLE_FLAG_STATUS = MenuItemFlagStatus.no_flag;

    private final PlaceRepository placeRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public Page<UserPlaceListResponse> getPlaces(UUID userId, String role, Pageable pageable) {
        verifyUserRole(userId, role);

        return placeRepository.findByApprovalStatusAndHiddenFalseOrderByCreatedAtDesc(STATUS_APPROVED, pageable)
                .map(this::toPlaceListResponse);
    }

    @Transactional(readOnly = true)
    public UserPlaceDetailResponse getPlaceWithMenu(UUID userId, String role, UUID placeId) {
        verifyUserRole(userId, role);

        Place place = getVisiblePlace(placeId);
        return toPlaceDetailResponse(place, getVisibleGroupedMenu(place.getId()));
    }

    @Transactional(readOnly = true)
    public UserMenuItemResponse getMenuItem(UUID userId, String role, UUID itemId) {
        verifyUserRole(userId, role);

        MenuItem item = menuItemRepository.findByIdAndAvailableTrueAndFlaggedStatus(itemId, VISIBLE_FLAG_STATUS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        getVisiblePlace(item.getPlaceId());

        return toMenuItemResponse(item);
    }

    private void verifyUserRole(UUID userId, String role) {
        if (!USER_ROLE.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only user role can access places");
        }

        if (!userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have user role");
        }
    }

    private Place getVisiblePlace(UUID placeId) {
        return placeRepository.findByIdAndApprovalStatusAndHiddenFalse(placeId, STATUS_APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found"));
    }

    private List<UserMenuGroupResponse> getVisibleGroupedMenu(UUID placeId) {
        List<MenuCategory> categories = menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId);
        List<MenuItem> items = menuItemRepository.findByPlaceIdAndAvailableTrueAndFlaggedStatusOrderByCreatedAtAsc(
                placeId,
                VISIBLE_FLAG_STATUS
        );

        Map<UUID, List<UserMenuItemResponse>> itemsByCategory = new LinkedHashMap<>();
        for (MenuCategory category : categories) {
            itemsByCategory.put(category.getId(), new ArrayList<>());
        }

        List<UserMenuItemResponse> uncategorizedItems = new ArrayList<>();
        for (MenuItem item : items) {
            UserMenuItemResponse response = toMenuItemResponse(item);
            if (item.getCategoryId() != null && itemsByCategory.containsKey(item.getCategoryId())) {
                itemsByCategory.get(item.getCategoryId()).add(response);
            } else {
                uncategorizedItems.add(response);
            }
        }

        List<UserMenuGroupResponse> groupedMenu = new ArrayList<>();
        for (MenuCategory category : categories) {
            groupedMenu.add(UserMenuGroupResponse.builder()
                    .category(toCategoryResponse(category))
                    .items(itemsByCategory.get(category.getId()))
                    .build());
        }

        groupedMenu.add(UserMenuGroupResponse.builder()
                .category(null)
                .items(uncategorizedItems)
                .build());

        return groupedMenu;
    }

    private UserPlaceListResponse toPlaceListResponse(Place place) {
        return UserPlaceListResponse.builder()
                .id(place.getId())
                .displayName(place.getDisplayName())
                .description(place.getDescription())
                .pureVeg(place.isPureVeg())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .build();
    }

    private UserPlaceDetailResponse toPlaceDetailResponse(Place place, List<UserMenuGroupResponse> menu) {
        return UserPlaceDetailResponse.builder()
                .id(place.getId())
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
                .menu(menu)
                .build();
    }

    private MenuCategoryResponse toCategoryResponse(MenuCategory category) {
        return MenuCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private UserMenuItemResponse toMenuItemResponse(MenuItem item) {
        return UserMenuItemResponse.builder()
                .id(item.getId())
                .placeId(item.getPlaceId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .description(item.getDescription())
                .mediaId(item.getMediaId())
                .price(item.getPrice())
                .veg(item.isVeg())
                .available(item.isAvailable())
                .build();
    }
}
