package com.amt_softwares.unnamed.place.repository;

import com.amt_softwares.unnamed.place.entity.PlaceChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaceChangeRepository extends JpaRepository<PlaceChange, UUID> {
    boolean existsByOwnerIdAndPlaceIdIsNullAndStatusIn(UUID ownerId, Collection<String> statuses);
    List<PlaceChange> findByStatusOrderByCreatedAtDesc(String status);
    List<PlaceChange> findByStatusInOrderByCreatedAtDesc(Collection<String> statuses);
    Optional<PlaceChange> findByIdAndStatus(UUID id, String status);
    Optional<PlaceChange> findByIdAndStatusIn(UUID id, Collection<String> statuses);
    Optional<PlaceChange> findFirstByOwnerIdAndPlaceIdIsNullAndStatusInOrderByCreatedAtDesc(UUID ownerId, Collection<String> statuses);
    Optional<PlaceChange> findFirstByPlaceIdAndOwnerIdAndStatusInOrderByCreatedAtDesc(UUID placeId, UUID ownerId, Collection<String> statuses);
}
