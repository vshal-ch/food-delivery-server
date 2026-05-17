package com.amt_softwares.unnamed.menu.repository;

import com.amt_softwares.unnamed.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByPlaceIdOrderByCreatedAtAsc(UUID placeId);
    Optional<MenuItem> findByIdAndPlaceId(UUID id, UUID placeId);
}
