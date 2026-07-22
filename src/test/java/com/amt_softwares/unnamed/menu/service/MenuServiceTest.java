package com.amt_softwares.unnamed.menu.service;

import com.amt_softwares.unnamed.admin.menu.dto.AdminMenuGroupResponse;
import com.amt_softwares.unnamed.admin.menu.dto.AdminMenuItemResponse;
import com.amt_softwares.unnamed.admin.menu.dto.FlagMenuItemRequest;
import com.amt_softwares.unnamed.menu.dto.CreateMenuCategoryRequest;
import com.amt_softwares.unnamed.menu.dto.CreateMenuItemRequest;
import com.amt_softwares.unnamed.menu.dto.MenuCategoryResponse;
import com.amt_softwares.unnamed.menu.dto.MenuGroupResponse;
import com.amt_softwares.unnamed.menu.dto.MenuItemResponse;
import com.amt_softwares.unnamed.menu.dto.SetMenuItemAvailabilityRequest;
import com.amt_softwares.unnamed.menu.dto.UpdateMenuItemRequest;
import com.amt_softwares.unnamed.menu.entity.MenuCategory;
import com.amt_softwares.unnamed.menu.entity.MenuItem;
import com.amt_softwares.unnamed.menu.entity.MenuItemFlagStatus;
import com.amt_softwares.unnamed.menu.repository.MenuCategoryRepository;
import com.amt_softwares.unnamed.menu.repository.MenuItemRepository;
import com.amt_softwares.unnamed.place.entity.Place;
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
class MenuServiceTest {

    private static final String PLACE_ROLE = "place";

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private MenuService menuService;
    private UUID userId;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(placeRepository, userRoleRepository, menuCategoryRepository, menuItemRepository);
        userId = UUID.randomUUID();
        placeId = UUID.randomUUID();
    }

    @Test
    void createCategorySavesTrimmedName() {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest();
        request.setName("  Starters  ");

        stubOwnerPlace();
        when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuCategoryResponse response = menuService.createCategory(userId, PLACE_ROLE, request);

        ArgumentCaptor<MenuCategory> categoryCaptor = ArgumentCaptor.forClass(MenuCategory.class);
        verify(menuCategoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getPlaceId()).isEqualTo(placeId);
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("Starters");
        assertThat(response.getName()).isEqualTo("Starters");
    }

    @Test
    void createMenuItemDefaultsAvailableAndNoFlag() {
        UUID categoryId = UUID.randomUUID();
        CreateMenuItemRequest request = createMenuItemRequest();
        request.setCategoryId(categoryId);
        request.setAvailable(null);

        stubOwnerPlace();
        when(menuCategoryRepository.existsByIdAndPlaceId(categoryId, placeId)).thenReturn(true);
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuService.createMenuItem(userId, PLACE_ROLE, request);

        ArgumentCaptor<MenuItem> itemCaptor = ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository).save(itemCaptor.capture());
        MenuItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getName()).isEqualTo("Paneer Tikka");
        assertThat(savedItem.getDescription()).isNull();
        assertThat(savedItem.getCategoryId()).isEqualTo(categoryId);
        assertThat(savedItem.isAvailable()).isTrue();
        assertThat(savedItem.getFlaggedStatus()).isEqualTo(MenuItemFlagStatus.no_flag);
        assertThat(response.getFlaggedStatus()).isEqualTo("no_flag");
    }

    @Test
    void updateMenuItemMovesFlaggedItemToSubmittedForReview() {
        UUID itemId = UUID.randomUUID();
        MenuItem item = menuItem(itemId, MenuItemFlagStatus.flagged, UUID.randomUUID());
        UpdateMenuItemRequest request = updateMenuItemRequest();
        request.setDescription("  ");

        stubOwnerPlace();
        when(menuItemRepository.findByIdAndPlaceId(itemId, placeId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuService.updateMenuItem(userId, PLACE_ROLE, itemId, request);

        assertThat(item.getFlaggedStatus()).isEqualTo(MenuItemFlagStatus.submitted_for_review);
        assertThat(item.getDescription()).isNull();
        assertThat(response.getFlaggedStatus()).isEqualTo("submitted_for_review");
    }

    @Test
    void setMenuItemAvailabilityAlsoMarksFlaggedItemForReview() {
        UUID itemId = UUID.randomUUID();
        MenuItem item = menuItem(itemId, MenuItemFlagStatus.flagged, UUID.randomUUID());
        SetMenuItemAvailabilityRequest request = new SetMenuItemAvailabilityRequest();
        request.setAvailable(false);

        stubOwnerPlace();
        when(menuItemRepository.findByIdAndPlaceId(itemId, placeId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuService.setMenuItemAvailability(userId, PLACE_ROLE, itemId, request);

        assertThat(response.isAvailable()).isFalse();
        assertThat(item.getFlaggedStatus()).isEqualTo(MenuItemFlagStatus.submitted_for_review);
    }

    @Test
    void flagMenuItemForAdminRequiresReasonWhenFlagging() {
        UUID itemId = UUID.randomUUID();
        FlagMenuItemRequest request = new FlagMenuItemRequest();
        request.setFlaggedStatus("flagged");
        request.setFlaggedReason("   ");

        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem(itemId, MenuItemFlagStatus.no_flag, null)));

        assertThatThrownBy(() -> menuService.flagMenuItemForAdmin(itemId, UUID.randomUUID(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void flagMenuItemForAdminClearsFlagStateWhenNoFlagRequested() {
        UUID itemId = UUID.randomUUID();
        MenuItem item = menuItem(itemId, MenuItemFlagStatus.flagged, UUID.randomUUID());
        item.setFlaggedReason("Old reason");
        FlagMenuItemRequest request = new FlagMenuItemRequest();
        request.setFlaggedStatus("no_flag");

        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminMenuItemResponse response = menuService.flagMenuItemForAdmin(itemId, UUID.randomUUID(), request);

        assertThat(response.getFlaggedStatus()).isEqualTo("no_flag");
        assertThat(response.getFlaggedReason()).isNull();
        assertThat(response.getFlaggedBy()).isNull();
    }

    @Test
    void getGroupedMenuGroupsItemsByCategoryAndUncategorized() {
        UUID categoryId = UUID.randomUUID();
        MenuCategory category = MenuCategory.builder()
                .id(categoryId)
                .placeId(placeId)
                .name("Main Course")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        MenuItem categorized = menuItem(UUID.randomUUID(), MenuItemFlagStatus.no_flag, categoryId);
        MenuItem uncategorized = menuItem(UUID.randomUUID(), MenuItemFlagStatus.no_flag, null);

        stubOwnerPlace();
        when(menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId)).thenReturn(List.of(category));
        when(menuItemRepository.findByPlaceIdOrderByCreatedAtAsc(placeId)).thenReturn(List.of(categorized, uncategorized));

        List<MenuGroupResponse> groups = menuService.getGroupedMenu(userId, PLACE_ROLE);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getCategory().getName()).isEqualTo("Main Course");
        assertThat(groups.get(0).getItems()).hasSize(1);
        assertThat(groups.get(1).getCategory()).isNull();
        assertThat(groups.get(1).getItems()).hasSize(1);
    }

    @Test
    void getGroupedMenuForAdminRejectsUnknownPlace() {
        when(placeRepository.existsById(placeId)).thenReturn(false);

        assertThatThrownBy(() -> menuService.getGroupedMenuForAdmin(placeId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getGroupedMenuForAdminReturnsAdminGrouping() {
        UUID categoryId = UUID.randomUUID();
        MenuCategory category = MenuCategory.builder()
                .id(categoryId)
                .placeId(placeId)
                .name("Desserts")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        MenuItem item = menuItem(UUID.randomUUID(), MenuItemFlagStatus.flagged, categoryId);

        when(placeRepository.existsById(placeId)).thenReturn(true);
        when(menuCategoryRepository.findByPlaceIdOrderByCreatedAtAsc(placeId)).thenReturn(List.of(category));
        when(menuItemRepository.findByPlaceIdOrderByCreatedAtAsc(placeId)).thenReturn(List.of(item));

        List<AdminMenuGroupResponse> groups = menuService.getGroupedMenuForAdmin(placeId);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getItems().get(0).getFlaggedStatus()).isEqualTo("flagged");
    }

    @Test
    void ownerMenuActionsRejectWrongRole() {
        assertThatThrownBy(() -> menuService.getGroupedMenu(userId, "user"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void stubOwnerPlace() {
        when(userRoleRepository.existsByUserIdAndRole(userId, PLACE_ROLE)).thenReturn(true);
        when(placeRepository.findByOwnerId(userId)).thenReturn(Optional.of(
                Place.builder()
                        .id(placeId)
                        .ownerId(userId)
                        .displayName("Owner Place")
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .updatedAt(LocalDateTime.now().minusDays(1))
                        .build()
        ));
    }

    private CreateMenuItemRequest createMenuItemRequest() {
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setName("  Paneer Tikka  ");
        request.setDescription("  ");
        request.setPrice(new BigDecimal("199.00"));
        request.setVeg(true);
        return request;
    }

    private UpdateMenuItemRequest updateMenuItemRequest() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();
        request.setName("  Updated Paneer Tikka  ");
        request.setDescription("Updated");
        request.setPrice(new BigDecimal("249.00"));
        request.setVeg(false);
        request.setAvailable(true);
        return request;
    }

    private MenuItem menuItem(UUID itemId, MenuItemFlagStatus flaggedStatus, UUID categoryId) {
        return MenuItem.builder()
                .id(itemId)
                .placeId(placeId)
                .categoryId(categoryId)
                .name("Item")
                .description("Desc")
                .price(new BigDecimal("100.00"))
                .veg(true)
                .available(true)
                .flaggedStatus(flaggedStatus)
                .flaggedBy(UUID.randomUUID())
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
