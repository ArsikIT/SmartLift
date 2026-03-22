package com.smartlift.service.impl;

import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.RoleRepository;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void getAllUsers_returnsUsersInSameOrganization() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User otherUser = createUserInOrg(2L, "user2", 10L);
        Page<User> page = new PageImpl<>(List.of(admin, otherUser));
        when(userRepository.findAllByOrganizationId(10L, pageable)).thenReturn(page);

        Page<UserResponse> result = userService.getAllUsers("admin", pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void getUserById_returnsUser() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User target = createUserInOrg(2L, "target", 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        UserResponse result = userService.getUserById("admin", 2L);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("target");
    }

    @Test
    void getUserById_throwsWhenDifferentOrganization() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User target = createUserInOrg(2L, "other", 99L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.getUserById("admin", 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("another organization");
    }

    @Test
    void getUserById_throwsWhenUserNotFound() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("admin", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createUser_createsUserInSameOrganization() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        Role svcRole = new Role();
        svcRole.setId(3L);
        svcRole.setName(RoleName.SERVICE);
        when(roleRepository.findByName(RoleName.SERVICE)).thenReturn(Optional.of(svcRole));

        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        User savedUser = createUserInOrg(5L, "newuser", 10L);
        savedUser.setEmail("new@test.com");
        savedUser.setRoles(Set.of(svcRole));
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        UserResponse result = userService.createUser("admin", request);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getUsername()).isEqualTo("newuser");
    }

    @Test
    void createUser_throwsWhenUsernameExists() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User existing = createUserInOrg(99L, "taken", 10L);
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(existing));

        UserRequest request = new UserRequest();
        request.setUsername("taken");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        assertThatThrownBy(() -> userService.createUser("admin", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void createUser_throwsWhenEmailExists() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        User existing = createUserInOrg(99L, "other", 10L);
        existing.setEmail("taken@test.com");
        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(existing));

        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setEmail("taken@test.com");
        request.setPassword("password123");

        assertThatThrownBy(() -> userService.createUser("admin", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateUser_updatesUserFields() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User target = createUserInOrg(2L, "oldname", 10L);
        target.setEmail("old@test.com");
        target.setRoles(Set.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(target);

        UserRequest request = new UserRequest();
        request.setUsername("newname");
        request.setEmail("new@test.com");
        request.setPassword("newpass123");
        request.setEnabled(false);

        userService.updateUser("admin", 2L, request);

        assertThat(target.getUsername()).isEqualTo("newname");
        assertThat(target.getEmail()).isEqualTo("new@test.com");
        assertThat(target.isEnabled()).isFalse();
    }

    @Test
    void deleteUser_deletesUser() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User target = createUserInOrg(2L, "target", 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.deleteUser("admin", 2L);

        verify(userRepository).delete(target);
        verify(userRepository).flush();
    }

    @Test
    void deleteUser_throwsWhenDeletingSelf() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.deleteUser("admin", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete yourself");
    }

    @Test
    void deleteUser_throwsWhenUserHasReferences() {
        User admin = createAdmin(1L, "admin", 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        User target = createUserInOrg(2L, "referenced", 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        doNothing().when(userRepository).delete(target);
        doThrow(new DataIntegrityViolationException("FK")).when(userRepository).flush();

        assertThatThrownBy(() -> userService.deleteUser("admin", 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("referenced by events");
    }

    @Test
    void getAdminOrThrow_throwsWhenUserHasNoOrganization() {
        User user = new User();
        user.setId(1L);
        user.setUsername("orphan");
        user.setOrganization(null);
        when(userRepository.findByUsername("orphan")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getAllUsers("orphan", pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User has no organization");
    }

    private User createAdmin(Long id, String username, Long orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        org.setType(OrganizationType.SERVICE);

        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setOrganization(org);
        user.setRoles(Set.of(adminRole));
        user.setEnabled(true);
        return user;
    }

    private User createUserInOrg(Long id, String username, Long orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        org.setType(OrganizationType.SERVICE);

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setOrganization(org);
        user.setRoles(new HashSet<>());
        user.setEnabled(true);
        return user;
    }
}
