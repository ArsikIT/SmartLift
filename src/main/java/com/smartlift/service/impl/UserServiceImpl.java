package com.smartlift.service.impl;

import com.smartlift.dto.UserRequest;
import com.smartlift.dto.UserResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.RoleName;
import com.smartlift.model.User;
import com.smartlift.repository.OrganizationRepository;
import com.smartlift.repository.RoleRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(SmartLiftMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return SmartLiftMapper.toUserResponse(getOrThrow(id));
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        User user = new User();
        applyRequest(user, request);
        return saveAndMap(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = getOrThrow(id);
        applyRequest(user, request);
        return saveAndMap(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = getOrThrow(id);
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("User cannot be deleted while referenced by events, maintenances, or documents");
        }
    }

    private void applyRequest(User user, UserRequest request) {
        validateUniqueUsername(request.getUsername(), user.getId());
        validateUniqueEmail(request.getEmail(), user.getId());

        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(request.getPassword());
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        user.setOrganization(resolveOrganization(request.getOrganizationId()));
        user.setRoles(resolveRoles(request.getRoleNames()));
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

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            return null;
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return new HashSet<>();
        }
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            RoleName roleName;
            try {
                roleName = RoleName.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Role not found: " + name);
            }
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
            roles.add(role);
        }
        return roles;
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
