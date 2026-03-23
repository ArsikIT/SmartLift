package com.smartlift.service.impl;

import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Lift;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextHelperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityContextHelper securityHelper;

    @Test
    void resolveUser_returnsUserWithOrganization() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("TestOrg");

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setOrganization(org);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        User result = securityHelper.resolveUser("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getOrganization().getId()).isEqualTo(1L);
    }

    @Test
    void resolveUser_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityHelper.resolveUser("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: unknown");
    }

    @Test
    void resolveUser_throwsWhenUserHasNoOrganization() {
        User user = new User();
        user.setId(1L);
        user.setUsername("orphan");
        user.setOrganization(null);

        when(userRepository.findByUsername("orphan")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> securityHelper.resolveUser("orphan"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User has no organization");
    }

    @Test
    void checkLiftBelongsToOrg_passesWhenManufacturerMatches() {
        Lift lift = createLiftWithOrgs(1L, null, null);

        securityHelper.checkLiftBelongsToOrg(lift, 1L);
        // no exception
    }

    @Test
    void checkLiftBelongsToOrg_passesWhenServiceMatches() {
        Lift lift = createLiftWithOrgs(null, 2L, null);

        securityHelper.checkLiftBelongsToOrg(lift, 2L);
        // no exception
    }

    @Test
    void checkLiftBelongsToOrg_passesWhenManagementMatches() {
        Lift lift = createLiftWithOrgs(null, null, 3L);

        securityHelper.checkLiftBelongsToOrg(lift, 3L);
        // no exception
    }

    @Test
    void checkLiftBelongsToOrg_throwsWhenNoOrgMatches() {
        Lift lift = createLiftWithOrgs(1L, 2L, 3L);

        assertThatThrownBy(() -> securityHelper.checkLiftBelongsToOrg(lift, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void checkLiftBelongsToOrg_throwsWhenAllOrgsNull() {
        Lift lift = new Lift();

        assertThatThrownBy(() -> securityHelper.checkLiftBelongsToOrg(lift, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Lift createLiftWithOrgs(Long mfgOrgId, Long svcOrgId, Long mgmtOrgId) {
        Lift lift = new Lift();
        if (mfgOrgId != null) {
            Organization org = new Organization();
            org.setId(mfgOrgId);
            org.setType(OrganizationType.MANUFACTURER);
            lift.setManufacturerOrganization(org);
        }
        if (svcOrgId != null) {
            Organization org = new Organization();
            org.setId(svcOrgId);
            org.setType(OrganizationType.SERVICE);
            lift.setServiceOrganization(org);
        }
        if (mgmtOrgId != null) {
            Organization org = new Organization();
            org.setId(mgmtOrgId);
            org.setType(OrganizationType.MANAGEMENT);
            lift.setManagementOrganization(org);
        }
        return lift;
    }
}
