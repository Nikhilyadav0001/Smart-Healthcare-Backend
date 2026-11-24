package com.smarthealth.auth;

import com.smarthealth.auth.controller.AdminRoleController;
import com.smarthealth.auth.dto.AdminUserSummaryResponse;
import com.smarthealth.auth.dto.UpdateRoleRequest;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserRole;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminRoleControllerTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private AdminRoleController controller;

    private final UUID testUserId = UUID.randomUUID();
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        userRoleRepository = Mockito.mock(UserRoleRepository.class);
        controller = new AdminRoleController(userRepository, userRoleRepository);

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("patient@smarthealth.io");
        testUser.setStatus("ACTIVE");
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        testUser.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void testListAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(userRoleRepository.findByIdUserId(testUserId)).thenReturn(
                List.of(new UserRole(new UserRole.UserRoleId(testUserId, "USER")))
        );

        ResponseEntity<List<AdminUserSummaryResponse>> response = controller.listAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("patient@smarthealth.io", response.getBody().get(0).email());
        assertTrue(response.getBody().get(0).roles().contains("USER"));
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByIdUserId(testUserId)).thenReturn(
                List.of(new UserRole(new UserRole.UserRoleId(testUserId, "PATIENT")))
        );

        ResponseEntity<AdminUserSummaryResponse> response = controller.getUserById(testUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUserId, response.getBody().id());
        assertTrue(response.getBody().roles().contains("PATIENT"));
    }

    @Test
    void testGetUserById_NotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.getUserById(unknownId));
    }

    @Test
    void testUpdateUserRoles_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRoleRepository.findByIdUserId(testUserId)).thenReturn(
                List.of(new UserRole(new UserRole.UserRoleId(testUserId, "DOCTOR")))
        );

        UpdateRoleRequest request = new UpdateRoleRequest(List.of("ROLE_DOCTOR"));
        ResponseEntity<AdminUserSummaryResponse> response = controller.updateUserRoles(testUserId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userRoleRepository).deleteByIdUserId(testUserId);
        verify(userRoleRepository).flush();
        verify(userRoleRepository).save(any(UserRole.class));
        assertTrue(response.getBody().roles().contains("DOCTOR"));
    }

    @Test
    void testSetSingleRole_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRoleRepository.findByIdUserId(testUserId)).thenReturn(
                List.of(new UserRole(new UserRole.UserRoleId(testUserId, "ADMIN")))
        );

        ResponseEntity<AdminUserSummaryResponse> response = controller.setSingleRole(testUserId, "ADMIN");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userRoleRepository).deleteByIdUserId(testUserId);
        assertTrue(response.getBody().roles().contains("ADMIN"));
    }
}
