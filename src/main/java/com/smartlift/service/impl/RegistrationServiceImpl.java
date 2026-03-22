package com.smartlift.service.impl;

import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.OrganizationRepository;
import com.smartlift.repository.RoleRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.RegistrationService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(RegisterRequest request) {
        validateUniqueness(request);
        Organization organization = createOrganization(request);
        User user = createAdminUser(request, organization);
        return SmartLiftMapper.toUserResponse(user);
    }

    private void validateUniqueness(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        if (organizationRepository.findByName(request.getOrganizationName()).isPresent()) {
            throw new ConflictException("Organization already exists: " + request.getOrganizationName());
        }
    }

    private Organization createOrganization(RegisterRequest request) {
        OrganizationType orgType = parseOrganizationType(request.getOrganizationType());

        Organization organization = new Organization();
        organization.setName(request.getOrganizationName().trim());
        organization.setType(orgType);
        return organizationRepository.save(organization);
    }

    private User createAdminUser(RegisterRequest request, Organization organization) {
        RoleName orgRole = organization.getType().toRoleName();

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setOrganization(organization);
        user.setRoles(Set.of(findRole(RoleName.ADMIN), findRole(orgRole)));

        return userRepository.save(user);
    }

    private OrganizationType parseOrganizationType(String type) {
        try {
            return OrganizationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid organization type: " + type
                    + ". Allowed: MANUFACTURER, SERVICE, MANAGEMENT");
        }
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }
}
