package com.amt_softwares.unnamed.menu.service;

import com.amt_softwares.unnamed.menu.dto.CreateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.CreateMenuItemRequest;
import com.amt_softwares.unnamed.menu.dto.FlagMenuItemRequest;
import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import com.amt_softwares.unnamed.menu.dto.MenuGroupResponse;
import com.amt_softwares.unnamed.menu.dto.MenuItemResponse;
import com.amt_softwares.unnamed.menu.dto.SetMenuItemAvailabilityRequest;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuItemRequest;
import com.amt_softwares.unnamed.menu.entity.MenuCategory;
import com.amt_softwares.unnamed.menu.entity.MenuItem;
import com.amt_softwares.unnamed.menu.repository.MenuCategoryRepository;
import com.amt_softwares.unnamed.menu.repository.MenuItemRepository;
import com.amt_softwares.unnamed.place.entity.Place;
import com.amt_softwares.unnamed.place.repository.PlaceRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final String PLACE_ROLE = "place";
    private static final String STATUS_APPROVED = "approved";

    private final PlaceRepository placeRepository;
    private final UserRoleRepository userRoleRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    @Transactional
    public MenuCategoryResponse createCategory(UUID userId, String role, CreateMenuCategoryRequest request) {
        UUID placeId = getApprovedPlaceId(userId, role);
        LocalDateTime now = LocalDateTime.now();
        MenuCategory category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .name(cleanRequired(request.getName()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toCategoryResponse(menuCategoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getCategories(UUID userId, String role) {
        UUID placeId = getApprovedPlaceId(userId, role);
        return menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId)
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuCategoryResponse getCategory(UUID userId, String role, UUID categoryId) {
        UUID placeId = getApprovedPlaceId(userId, role);
        return toCategoryResponse(getOwnedCategory(placeId, categoryId));
    }

    @Transactional
    public MenuCategoryResponse updateCategory(UUID userId, String role, UUID categoryId, UpdateMenuCategoryRequest request) {
        UUID placeId = getApprovedPlaceId(userId, role);
        MenuCategory category = getOwnedCategory(placeId, categoryId);
        category.setName(cleanRequired(request.getName()));
        category.setUpdatedAt(LocalDateTime.now());
        return toCategoryResponse(menuCategoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID userId, String role, UUID categoryId) {
        UUID placeId = getApprovedPlaceId(userId, role);
        MenuCategory category = getOwnedCategory(placeId, categoryId);
        menuCategoryRepository.delete(category);
    }

    @Transactional
    public MenuItemResponse createMenuItem(UUID userId, String role, CreateMenuItemRequest request) {
        UUID placeId = getApprovedPlaceId(userId, role);
        UUID categoryId = validateOptionalCategory(placeId, request.getCategoryId());
        LocalDateTime now = LocalDateTime.now();
        MenuItem item = MenuItem.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .categoryId(categoryId)
                .name(cleanRequired(request.getName()))
                .description(cleanOptional(request.getDescription()))
                .mediaId(request.getMediaId())
                .price(request.getPrice())
                .veg(request.getVeg())
                .available(request.getAvailable() == null || request.getAvailable())
                .flagged(false)
                .flaggedReason(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(UUID userId, String role, UUID itemId, UpdateMenuItemRequest request) {
        UUID placeId = getApprovedPlaceId(userId, role);
        MenuItem item = getOwnedMenuItem(placeId, itemId);
        item.setName(cleanRequired(request.getName()));
        item.setDescription(cleanOptional(request.getDescription()));
        item.setMediaId(request.getMediaId());
        item.setPrice(request.getPrice());
        item.setVeg(request.getVeg());
        item.setAvailable(request.getAvailable());
        item.setUpdatedAt(LocalDateTime.now());

        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse setMenuItemAvailability(
            UUID userId,
            String role,
            UUID itemId,
            SetMenuItemAvailabilityRequest request
    ) {
        UUID placeId = getApprovedPlaceId(userId, role);
        MenuItem item = getOwnedMenuItem(placeId, itemId);
        item.setAvailable(request.getAvailable());
        item.setUpdatedAt(LocalDateTime.now());
        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse changeMenuItemCategory(UUID userId, String role, UUID itemId, UUID categoryId) {
        UUID placeId = getApprovedPlaceId(userId, role);
        MenuItem item = getOwnedMenuItem(placeId, itemId);
        item.setCategoryId(validateOptionalCategory(placeId, categoryId));
        item.setUpdatedAt(LocalDateTime.now());
        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse flagMenuItemForAdmin(UUID itemId, UUID adminId, FlagMenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));

        item.setFlagged(request.getFlagged());
        item.setFlaggedReason(request.getFlagged() ? cleanOptional(request.getFlaggedReason()) : null);
        item.setFlaggedBy(request.getFlagged() ? adminId : null);
        item.setUpdatedAt(LocalDateTime.now());

        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItem(UUID userId, String role, UUID itemId) {
        UUID placeId = getApprovedPlaceId(userId, role);
        return toItemResponse(getOwnedMenuItem(placeId, itemId));
    }

    @Transactional
    public void deleteMenuItem(UUID userId, String role, UUID itemId) {
        UUID placeId = getApprovedPlaceId(userId, role);
        menuItemRepository.delete(getOwnedMenuItem(placeId, itemId));
    }

    @Transactional(readOnly = true)
    public List<MenuGroupResponse> getGroupedMenu(UUID userId, String role) {
        UUID placeId = getApprovedPlaceId(userId, role);
        List<MenuCategory> categories = menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId);
        List<MenuItem> items = menuItemRepository.findByPlaceIdOrderByCreatedAtAsc(placeId);

        Map<UUID, List<MenuItemResponse>> itemsByCategory = new LinkedHashMap<>();
        for (MenuCategory category : categories) {
            itemsByCategory.put(category.getId(), new ArrayList<>());
        }
        List<MenuItemResponse> uncategorizedItems = new ArrayList<>();

        for (MenuItem item : items) {
            MenuItemResponse response = toItemResponse(item);
            if (item.getCategoryId() != null && itemsByCategory.containsKey(item.getCategoryId())) {
                itemsByCategory.get(item.getCategoryId()).add(response);
            } else {
                uncategorizedItems.add(response);
            }
        }

        List<MenuGroupResponse> groupedMenu = new ArrayList<>();
        for (MenuCategory category : categories) {
            groupedMenu.add(MenuGroupResponse.builder()
                    .category(toCategoryResponse(category))
                    .items(itemsByCategory.get(category.getId()))
                    .build());
        }

        groupedMenu.add(MenuGroupResponse.builder()
                .category(null)
                .items(uncategorizedItems)
                .build());

        return groupedMenu;
    }

    private UUID getApprovedPlaceId(UUID userId, String role) {
        if (!PLACE_ROLE.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only place users can manage menu");
        }

        if (!userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have place role");
        }

        Place place = placeRepository.findByOwnerId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved place not found"));

        if (!STATUS_APPROVED.equals(place.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Place must be approved before managing menu");
        }

        return place.getId();
    }

    private MenuCategory getOwnedCategory(UUID placeId, UUID categoryId) {
        return menuCategoryRepository.findByIdAndPlaceId(categoryId, placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu category not found"));
    }

    private MenuItem getOwnedMenuItem(UUID placeId, UUID itemId) {
        return menuItemRepository.findByIdAndPlaceId(itemId, placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    }

    private UUID validateOptionalCategory(UUID placeId, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        if (!menuCategoryRepository.existsByIdAndPlaceId(categoryId, placeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid menu category");
        }

        return categoryId;
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

    private MenuCategoryResponse toCategoryResponse(MenuCategory category) {
        return MenuCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private MenuItemResponse toItemResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .description(item.getDescription())
                .mediaId(item.getMediaId())
                .price(item.getPrice())
                .veg(item.isVeg())
                .available(item.isAvailable())
                .flagged(item.isFlagged())
                .flaggedReason(item.getFlaggedReason())
                .flaggedBy(item.getFlaggedBy())
                .build();
    }
}
