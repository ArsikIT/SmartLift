package com.smartlift.service.impl;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private SecurityContextHelper securityHelper;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    @Test
    void getMyOrganization_returnsOrgResponse() {
        Organization org = createOrganization(1L, "TestOrg", OrganizationType.MANUFACTURER);
        User user = createUserWithOrg("admin", org);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        OrganizationResponse result = organizationService.getMyOrganization("admin");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("TestOrg");
        assertThat(result.getType()).isEqualTo(OrganizationType.MANUFACTURER);
    }

    @Test
    void updateMyOrganization_updatesAndReturns() {
        Organization org = createOrganization(1L, "OldName", OrganizationType.SERVICE);
        User user = createUserWithOrg("admin", org);
        when(securityHelper.resolveUser("admin")).thenReturn(user);
        when(organizationRepository.findByName("NewName")).thenReturn(Optional.empty());

        Organization savedOrg = createOrganization(1L, "NewName", OrganizationType.SERVICE);
        savedOrg.setAddress("123 Main St");
        savedOrg.setContactEmail("new@test.com");
        savedOrg.setContactPhone("+1234567890");
        when(organizationRepository.saveAndFlush(any(Organization.class))).thenReturn(savedOrg);

        OrganizationRequest request = new OrganizationRequest();
        request.setName("NewName");
        request.setAddress("123 Main St");
        request.setContactEmail("new@test.com");
        request.setContactPhone("+1234567890");

        OrganizationResponse result = organizationService.updateMyOrganization("admin", request);

        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        assertThat(result.getContactEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateMyOrganization_allowsSameName() {
        Organization org = createOrganization(1L, "SameName", OrganizationType.SERVICE);
        User user = createUserWithOrg("admin", org);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        // findByName returns the same org (same ID)
        when(organizationRepository.findByName("SameName")).thenReturn(Optional.of(org));
        when(organizationRepository.saveAndFlush(any(Organization.class))).thenReturn(org);

        OrganizationRequest request = new OrganizationRequest();
        request.setName("SameName");

        OrganizationResponse result = organizationService.updateMyOrganization("admin", request);

        assertThat(result.getName()).isEqualTo("SameName");
    }

    @Test
    void updateMyOrganization_throwsWhenNameTakenByAnotherOrg() {
        Organization org = createOrganization(1L, "MyOrg", OrganizationType.SERVICE);
        User user = createUserWithOrg("admin", org);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Organization otherOrg = createOrganization(2L, "TakenName", OrganizationType.MANUFACTURER);
        when(organizationRepository.findByName("TakenName")).thenReturn(Optional.of(otherOrg));

        OrganizationRequest request = new OrganizationRequest();
        request.setName("TakenName");

        assertThatThrownBy(() -> organizationService.updateMyOrganization("admin", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Organization name already exists");
    }

    private Organization createOrganization(Long id, String name, OrganizationType type) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        org.setType(type);
        return org;
    }

    private User createUserWithOrg(String username, Organization org) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setOrganization(org);
        return user;
    }
}
