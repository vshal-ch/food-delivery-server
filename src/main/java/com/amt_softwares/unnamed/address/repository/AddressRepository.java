package com.amt_softwares.unnamed.address.repository;

import com.amt_softwares.unnamed.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(UUID userId);
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserId(UUID userId);

    @Modifying
    @Query("update Address address set address.defaultAddress = false where address.userId = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
