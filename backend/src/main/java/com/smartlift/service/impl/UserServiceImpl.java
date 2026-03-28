package com.smartlift.service.impl;

import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.RoleRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.UserService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String currentUsername, Pageable pageable) {
        User admin = getAdminOrThrow(currentUsername);
        return userRepository.findAllByOrganizationId(admin.getOrganization().getId(), pageable)
                .map(SmartLiftMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String currentUsername, Long id) {
        User admin = getAdminOrThrow(currentUsername);
        User user = getOrThrow(id);
        checkSameOrganization(admin, user);
        return SmartLiftMapper.toUserResponse(user);
    }

    @Override
    public UserResponse createUser(String currentUsername, UserRequest request) {
        User admin = getAdminOrThrow(currentUsername);

        validateUniqueUsername(request.getUsername(), null);
        validateUniqueEmail(request.getEmail(), null);

        Organization organization = admin.getOrganization();
        RoleName orgRole = organization.getType().toRoleName();

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        user.setOrganization(organization);

        Set<Role> roles = new HashSet<>();
        roles.add(findRole(orgRole));
        user.setRoles(roles);

        return saveAndMap(user);
    }

    @Override
    public UserResponse updateUser(String currentUsername, Long id, UserRequest request) {
        User admin = getAdminOrThrow(currentUsername);
        User user = getOrThrow(id);
        checkSameOrganization(admin, user);

        validateUniqueUsername(request.getUsername(), user.getId());
        validateUniqueEmail(request.getEmail(), user.getId());

        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        return saveAndMap(user);
    }

    @Override
    public void deleteUser(String currentUsername, Long id) {
        User admin = getAdminOrThrow(currentUsername);
        User user = getOrThrow(id);
        checkSameOrganization(admin, user);

        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("Cannot delete yourself");
        }

        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("User cannot be deleted while referenced by events, maintenances, or documents");
        }
    }

    private User getAdminOrThrow(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (user.getOrganization() == null) {
            throw new BadRequestException("User has no organization");
        }

        return user;
    }

    private void checkSameOrganization(User admin, User target) {
        if (!admin.getOrganization().getId().equals(target.getOrganization().getId())) {
            throw new AccessDeniedException("Access denied: user belongs to another organization");
        }
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }

    private void validateUniqueUsername(String username, Long currentId) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Username already exists: " + username);
            }
        });
    }

    private void validateUniqueEmail(String email, Long currentId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Email already exists: " + email);
            }
        });
    }

    private UserResponse saveAndMap(User user) {
        try {
            User saved = userRepository.saveAndFlush(user);
            return SmartLiftMapper.toUserResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username or email already exists");
        }
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
