package com.smartlift.service.impl;

import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.OrganizationRepository;
import com.smartlift.repository.RoleRepository;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void register_createsUserAndOrganization() {
        RegisterRequest request = createRegisterRequest("admin", "admin@test.com", "password123", "TestOrg", "MANUFACTURER");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(organizationRepository.findByName("TestOrg")).thenReturn(Optional.empty());

        Organization savedOrg = new Organization();
        savedOrg.setId(1L);
        savedOrg.setName("TestOrg");
        savedOrg.setType(OrganizationType.MANUFACTURER);
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);

        Role adminRole = createRole(1L, RoleName.ADMIN);
        Role mfgRole = createRole(2L, RoleName.MANUFACTURER);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.MANUFACTURER)).thenReturn(Optional.of(mfgRole));

        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("admin");
        savedUser.setEmail("admin@test.com");
        savedUser.setEnabled(true);
        savedUser.setOrganization(savedOrg);
        savedUser.setRoles(java.util.Set.of(adminRole, mfgRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = registrationService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getEmail()).isEqualTo("admin@test.com");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getRoles()).containsExactlyInAnyOrder("ADMIN", "MANUFACTURER");
    }

    @Test
    void register_throwsWhenUsernameExists() {
        RegisterRequest request = createRegisterRequest("existing", "new@test.com", "password123", "Org", "SERVICE");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void register_throwsWhenEmailExists() {
        RegisterRequest request = createRegisterRequest("new", "existing@test.com", "password123", "Org", "SERVICE");

        when(userRepository.existsByUsername("new")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void register_throwsWhenOrganizationExists() {
        RegisterRequest request = createRegisterRequest("admin", "admin@test.com", "password123", "ExistingOrg", "SERVICE");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(organizationRepository.findByName("ExistingOrg")).thenReturn(Optional.of(new Organization()));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Organization already exists");
    }

    @Test
    void register_throwsForInvalidOrganizationType() {
        RegisterRequest request = createRegisterRequest("admin", "admin@test.com", "password123", "Org", "INVALID");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(organizationRepository.findByName("Org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid organization type");
    }

    @Test
    void register_throwsWhenRoleNotFound() {
        RegisterRequest request = createRegisterRequest("admin", "admin@test.com", "password123", "Org", "SERVICE");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(organizationRepository.findByName("Org")).thenReturn(Optional.empty());

        Organization savedOrg = new Organization();
        savedOrg.setId(1L);
        savedOrg.setName("Org");
        savedOrg.setType(OrganizationType.SERVICE);
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);

        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void register_createsServiceOrganizationWithServiceRole() {
        RegisterRequest request = createRegisterRequest("svcAdmin", "svc@test.com", "password123", "SvcOrg", "SERVICE");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(organizationRepository.findByName(anyString())).thenReturn(Optional.empty());

        Organization savedOrg = new Organization();
        savedOrg.setId(2L);
        savedOrg.setName("SvcOrg");
        savedOrg.setType(OrganizationType.SERVICE);
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);

        Role adminRole = createRole(1L, RoleName.ADMIN);
        Role svcRole = createRole(3L, RoleName.SERVICE);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.SERVICE)).thenReturn(Optional.of(svcRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("svcAdmin");
        savedUser.setEmail("svc@test.com");
        savedUser.setEnabled(true);
        savedUser.setOrganization(savedOrg);
        savedUser.setRoles(java.util.Set.of(adminRole, svcRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = registrationService.register(request);

        assertThat(response.getRoles()).containsExactlyInAnyOrder("ADMIN", "SERVICE");
    }

    private RegisterRequest createRegisterRequest(String username, String email, String password,
                                                   String orgName, String orgType) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setOrganizationName(orgName);
        request.setOrganizationType(orgType);
        return request;
    }

    private Role createRole(Long id, RoleName name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
