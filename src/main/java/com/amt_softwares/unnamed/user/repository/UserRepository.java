package com.amt_softwares.unnamed.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.amt_softwares.unnamed.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findById(UUID id);
    Optional<User> findByPhone(String Phone);
}
