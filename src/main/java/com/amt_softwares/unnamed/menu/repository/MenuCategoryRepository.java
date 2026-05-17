package com.amt_softwares.unnamed.menu.repository;

import com.amt_softwares.unnamed.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {
    List<MenuCategory> findByPlaceIdOrderByCreatedAtAsc(UUID placeId);
    Optional<MenuCategory> findByIdAndPlaceId(UUID id, UUID placeId);
    boolean existsByIdAndPlaceId(UUID id, UUID placeId);
}
