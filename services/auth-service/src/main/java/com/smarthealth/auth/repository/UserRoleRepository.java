package com.smarthealth.auth.repository;

import com.smarthealth.auth.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRole.UserRoleId> {

    List<UserRole> findByIdUserId(UUID userId);
}