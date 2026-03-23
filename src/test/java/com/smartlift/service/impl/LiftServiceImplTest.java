package com.smartlift.service.impl;

import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.LiftResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Lift;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.OrganizationRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiftServiceImplTest {

    @Mock
    private LiftRepository liftRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private SecurityContextHelper securityHelper;

    @InjectMocks
    private LiftServiceImpl liftService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void getAllLifts_returnsPageOfLifts() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(1L, "SN-001", "ModelX");
        Page<Lift> page = new PageImpl<>(List.of(lift));
        when(liftRepository.findAllByOrganizationId(10L, pageable)).thenReturn(page);

        Page<LiftResponse> result = liftService.getAllLifts("admin", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSerialNumber()).isEqualTo("SN-001");
    }

    @Test
    void getLiftById_returnsLiftResponse() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005", "ModelY");
        when(liftRepository.findDetailedById(5L)).thenReturn(Optional.of(lift));

        LiftResponse result = liftService.getLiftById("admin", 5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getSerialNumber()).isEqualTo("SN-005");
    }

    @Test
    void getLiftById_throwsWhenNotFound() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);
        when(liftRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liftService.getLiftById("admin", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lift not found");
    }

    @Test
    void createLift_createsAndReturnsLift() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-NEW");
        request.setModel("NewModel");
        request.setManufacturer("Acme");

        when(liftRepository.findBySerialNumber("SN-NEW")).thenReturn(Optional.empty());

        Lift savedLift = createLift(100L, "SN-NEW", "NewModel");
        when(liftRepository.saveAndFlush(any(Lift.class))).thenReturn(savedLift);
        when(liftRepository.findDetailedById(100L)).thenReturn(Optional.of(savedLift));

        LiftResponse result = liftService.createLift("admin", request);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getSerialNumber()).isEqualTo("SN-NEW");
    }

    @Test
    void createLift_throwsWhenSerialNumberExists() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-EXISTING");
        request.setModel("Model");

        Lift existingLift = createLift(50L, "SN-EXISTING", "OtherModel");
        when(liftRepository.findBySerialNumber("SN-EXISTING")).thenReturn(Optional.of(existingLift));

        assertThatThrownBy(() -> liftService.createLift("admin", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("serial number already exists");
    }

    @Test
    void createLift_resolvesOrganizations() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Organization mfgOrg = createOrganization(20L, OrganizationType.MANUFACTURER);
        Organization svcOrg = createOrganization(21L, OrganizationType.SERVICE);
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(mfgOrg));
        when(organizationRepository.findById(21L)).thenReturn(Optional.of(svcOrg));

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-ORG");
        request.setModel("Model");
        request.setManufacturerOrganizationId(20L);
        request.setServiceOrganizationId(21L);

        when(liftRepository.findBySerialNumber("SN-ORG")).thenReturn(Optional.empty());

        Lift savedLift = createLift(101L, "SN-ORG", "Model");
        when(liftRepository.saveAndFlush(any(Lift.class))).thenReturn(savedLift);
        when(liftRepository.findDetailedById(101L)).thenReturn(Optional.of(savedLift));

        LiftResponse result = liftService.createLift("admin", request);

        assertThat(result).isNotNull();
    }

    @Test
    void createLift_throwsWhenOrgTypeDoesNotMatch() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        // Service org referenced as manufacturer
        Organization svcOrg = createOrganization(21L, OrganizationType.SERVICE);
        when(organizationRepository.findById(21L)).thenReturn(Optional.of(svcOrg));

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-BAD");
        request.setModel("Model");
        request.setManufacturerOrganizationId(21L);

        when(liftRepository.findBySerialNumber("SN-BAD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liftService.createLift("admin", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must reference a MANUFACTURER organization");
    }

    @Test
    void updateLift_updatesAndReturnsLift() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift existingLift = createLift(5L, "SN-OLD", "OldModel");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(existingLift));

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-UPDATED");
        request.setModel("UpdatedModel");

        when(liftRepository.findBySerialNumber("SN-UPDATED")).thenReturn(Optional.empty());

        Lift savedLift = createLift(5L, "SN-UPDATED", "UpdatedModel");
        when(liftRepository.saveAndFlush(any(Lift.class))).thenReturn(savedLift);
        when(liftRepository.findDetailedById(5L)).thenReturn(Optional.of(savedLift));

        LiftResponse result = liftService.updateLift("admin", 5L, request);

        assertThat(result.getSerialNumber()).isEqualTo("SN-UPDATED");
    }

    @Test
    void updateLift_allowsSameSerialNumber() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift existingLift = createLift(5L, "SN-SAME", "Model");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(existingLift));
        when(liftRepository.findBySerialNumber("SN-SAME")).thenReturn(Optional.of(existingLift));

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-SAME");
        request.setModel("UpdatedModel");

        Lift savedLift = createLift(5L, "SN-SAME", "UpdatedModel");
        when(liftRepository.saveAndFlush(any(Lift.class))).thenReturn(savedLift);
        when(liftRepository.findDetailedById(5L)).thenReturn(Optional.of(savedLift));

        LiftResponse result = liftService.updateLift("admin", 5L, request);

        assertThat(result.getModel()).isEqualTo("UpdatedModel");
    }

    @Test
    void deleteLift_deletesLift() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-DEL", "Model");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        liftService.deleteLift("admin", 5L);

        verify(liftRepository).delete(lift);
        verify(liftRepository).flush();
    }

    @Test
    void deleteLift_throwsWhenRelatedDataExists() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-REF", "Model");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));
        doNothing().when(liftRepository).delete(lift);
        doThrow(new DataIntegrityViolationException("FK constraint")).when(liftRepository).flush();

        assertThatThrownBy(() -> liftService.deleteLift("admin", 5L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("related events, maintenance, or documents exist");
    }

    private User createUserWithOrg(Long userId, String username, Long orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setOrganization(org);
        return user;
    }

    private Lift createLift(Long id, String serialNumber, String model) {
        Lift lift = new Lift();
        lift.setId(id);
        lift.setSerialNumber(serialNumber);
        lift.setModel(model);
        lift.setStatus(LiftStatus.CREATED);
        return lift;
    }

    private Organization createOrganization(Long id, OrganizationType type) {
        Organization org = new Organization();
        org.setId(id);
        org.setType(type);
        return org;
    }
}
