package com.amt_softwares.unnamed.place.repository;

import com.amt_softwares.unnamed.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {
    boolean existsByOwnerId(UUID ownerId);
    Optional<Place> findByOwnerId(UUID ownerId);
}
