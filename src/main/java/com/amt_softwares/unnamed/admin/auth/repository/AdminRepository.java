package com.amt_softwares.unnamed.admin.auth.repository;

import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByUsername(String username);
}
