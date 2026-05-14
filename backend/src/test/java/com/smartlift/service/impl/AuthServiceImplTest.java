package com.smartlift.service.impl;

import com.smartlift.support.TestPasswords;
import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.response.AuthResponse;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.UserRepository;
import com.smartlift.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_returnsAuthResponseWithToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword(TestPasswords.BASIC);

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token-123");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(createUser()));

        AuthResponse response = authService.login(request);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRoles()).containsExactlyInAnyOrder("ADMIN", "MANUFACTURER");
        assertThat(response.getOrganization()).isNotNull();
        assertThat(response.getOrganization().getId()).isEqualTo(15L);
    }

    @Test
    void login_throwsWhenCredentialsInvalid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword(TestPasswords.WRONG);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    private User createUser() {
        Organization organization = new Organization();
        organization.setId(15L);
        organization.setName("TestOrg");
        organization.setType(OrganizationType.MANUFACTURER);

        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        Role orgRole = new Role();
        orgRole.setName(RoleName.MANUFACTURER);

        User user = new User();
        user.setId(7L);
        user.setUsername("admin");
        user.setOrganization(organization);
        user.setRoles(Set.of(adminRole, orgRole));
        return user;
    }
}
