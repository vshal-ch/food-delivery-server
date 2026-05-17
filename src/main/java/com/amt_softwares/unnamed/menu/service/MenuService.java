package com.amt_softwares.unnamed.menu.service;

import com.amt_softwares.unnamed.admin.menu.dto.AdminMenuGroupResponse;
import com.amt_softwares.unnamed.admin.menu.dto.AdminMenuItemResponse;
import com.amt_softwares.unnamed.menu.dto.CreateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.CreateMenuItemRequest;
import com.amt_softwares.unnamed.admin.menu.dto.FlagMenuItemRequest;
import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import com.amt_softwares.unnamed.menu.dto.MenuGroupResponse;
import com.amt_softwares.unnamed.menu.dto.MenuItemResponse;
import com.amt_softwares.unnamed.menu.dto.SetMenuItemAvailabilityRequest;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuItemRequest;
import com.amt_softwares.unnamed.menu.entity.MenuCategory;
import com.amt_softwares.unnamed.menu.entity.MenuItem;
import com.amt_softwares.unnamed.menu.entity.MenuItemFlagStatus;
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
    private static final MenuItemFlagStatus FLAG_STATUS_NO_FLAG = MenuItemFlagStatus.no_flag;
    private static final MenuItemFlagStatus FLAG_STATUS_FLAGGED = MenuItemFlagStatus.flagged;
    private static final MenuItemFlagStatus FLAG_STATUS_SUBMITTED_FOR_REVIEW = MenuItemFlagStatus.submitted_for_review;

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
                .flaggedStatus(FLAG_STATUS_NO_FLAG)
                .flaggedReason(null)
                .flaggedBy(null)
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
        markSubmittedForReviewIfFlagged(item);
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
    public AdminMenuItemResponse flagMenuItemForAdmin(UUID itemId, UUID adminId, FlagMenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));

        MenuItemFlagStatus flaggedStatus = parseAdminFlagStatus(request.getFlaggedStatus());
        if (FLAG_STATUS_FLAGGED.equals(flaggedStatus)) {
            String reason = cleanOptional(request.getFlaggedReason());
            if (reason == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason is required when flagging an item");
            }
            item.setFlaggedStatus(FLAG_STATUS_FLAGGED);
            item.setFlaggedReason(reason);
            item.setFlaggedBy(adminId);
        } else if (FLAG_STATUS_NO_FLAG.equals(flaggedStatus)) {
            item.setFlaggedStatus(FLAG_STATUS_NO_FLAG);
            item.setFlaggedReason(null);
            item.setFlaggedBy(null);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flaggedStatus must be flagged or no_flag");
        }

        item.setUpdatedAt(LocalDateTime.now());

        return toAdminItemResponse(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<AdminMenuItemResponse> getFlaggedItems() {
        return menuItemRepository.findByFlaggedStatusOrderByUpdatedAtDesc(FLAG_STATUS_FLAGGED)
                .stream()
                .map(this::toAdminItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminMenuItemResponse> getSubmittedForReviewItems() {
        return menuItemRepository.findByFlaggedStatusOrderByUpdatedAtDesc(FLAG_STATUS_SUBMITTED_FOR_REVIEW)
                .stream()
                .map(this::toAdminItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminMenuGroupResponse> getGroupedMenuForAdmin(UUID placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found");
        }

        List<MenuCategory> categories = menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId);
        List<MenuItem> items = menuItemRepository.findByPlaceIdOrderByCreatedAtAsc(placeId);

        Map<UUID, List<AdminMenuItemResponse>> itemsByCategory = new LinkedHashMap<>();
        for (MenuCategory category : categories) {
            itemsByCategory.put(category.getId(), new ArrayList<>());
        }
        List<AdminMenuItemResponse> uncategorizedItems = new ArrayList<>();

        for (MenuItem item : items) {
            AdminMenuItemResponse response = toAdminItemResponse(item);
            if (item.getCategoryId() != null && itemsByCategory.containsKey(item.getCategoryId())) {
                itemsByCategory.get(item.getCategoryId()).add(response);
            } else {
                uncategorizedItems.add(response);
            }
        }

        List<AdminMenuGroupResponse> groupedMenu = new ArrayList<>();
        for (MenuCategory category : categories) {
            groupedMenu.add(AdminMenuGroupResponse.builder()
                    .category(toCategoryResponse(category))
                    .items(itemsByCategory.get(category.getId()))
                    .build());
        }

        groupedMenu.add(AdminMenuGroupResponse.builder()
                .category(null)
                .items(uncategorizedItems)
                .build());

        return groupedMenu;
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

    private void markSubmittedForReviewIfFlagged(MenuItem item) {
        if (FLAG_STATUS_FLAGGED.equals(item.getFlaggedStatus()) ||
                FLAG_STATUS_SUBMITTED_FOR_REVIEW.equals(item.getFlaggedStatus())) {
            item.setFlaggedStatus(FLAG_STATUS_SUBMITTED_FOR_REVIEW);
        }
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
                .flaggedStatus(item.getFlaggedStatus().name())
                .flaggedReason(item.getFlaggedReason())
                .build();
    }

    private AdminMenuItemResponse toAdminItemResponse(MenuItem item) {
        return AdminMenuItemResponse.builder()
                .id(item.getId())
                .placeId(item.getPlaceId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .description(item.getDescription())
                .mediaId(item.getMediaId())
                .price(item.getPrice())
                .veg(item.isVeg())
                .available(item.isAvailable())
                .flaggedStatus(item.getFlaggedStatus().name())
                .flaggedReason(item.getFlaggedReason())
                .flaggedBy(item.getFlaggedBy())
                .build();
    }

    private MenuItemFlagStatus parseAdminFlagStatus(String value) {
        String cleaned = cleanRequired(value).toLowerCase();
        if (FLAG_STATUS_FLAGGED.name().equals(cleaned)) {
            return FLAG_STATUS_FLAGGED;
        }

        if (FLAG_STATUS_NO_FLAG.name().equals(cleaned)) {
            return FLAG_STATUS_NO_FLAG;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flaggedStatus must be flagged or no_flag");
    }
}
