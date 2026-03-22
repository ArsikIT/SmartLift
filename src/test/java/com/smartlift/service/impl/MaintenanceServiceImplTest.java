package com.smartlift.service.impl;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.model.enums.MaintenanceStatus;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.MaintenanceRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceImplTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private LiftRepository liftRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContextHelper securityHelper;

    @InjectMocks
    private MaintenanceServiceImpl maintenanceService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void getAllMaintenances_returnsPage() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(1L, "SN-001");
        Maintenance m = createMaintenance(1L, lift, "Title", MaintenanceStatus.PENDING);
        Page<Maintenance> page = new PageImpl<>(List.of(m));
        when(maintenanceRepository.findAllByOrganizationId(10L, pageable)).thenReturn(page);

        Page<MaintenanceResponse> result = maintenanceService.getAllMaintenances("admin", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMaintenancesByLiftId_returnsFilteredPage() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        Maintenance m = createMaintenance(1L, lift, "Title", MaintenanceStatus.PENDING);
        Page<Maintenance> page = new PageImpl<>(List.of(m));
        when(maintenanceRepository.findAllByLiftId(5L, pageable)).thenReturn(page);

        Page<MaintenanceResponse> result = maintenanceService.getMaintenancesByLiftId("admin", 5L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMaintenanceById_returnsMaintenance() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        Maintenance m = createMaintenance(20L, lift, "Check", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(m));

        MaintenanceResponse result = maintenanceService.getMaintenanceById("admin", 20L);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getTitle()).isEqualTo("Check");
    }

    @Test
    void getMaintenanceById_throwsWhenNotFound() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);
        when(maintenanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.getMaintenanceById("admin", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createMaintenance_createsWithPendingStatus() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Annual check");
        request.setDescription("Yearly inspection");

        Maintenance saved = createMaintenance(30L, lift, "Annual check", MaintenanceStatus.PENDING);
        when(maintenanceRepository.saveAndFlush(any(Maintenance.class))).thenReturn(saved);

        MaintenanceResponse result = maintenanceService.createMaintenance("admin", request);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getTitle()).isEqualTo("Annual check");
    }

    @Test
    void createMaintenance_resolvesAssignedTechnician() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        User tech = new User();
        tech.setId(7L);
        tech.setUsername("technician");
        when(userRepository.findById(7L)).thenReturn(Optional.of(tech));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Repair");
        request.setAssignedTechnicianId(7L);

        Maintenance saved = createMaintenance(31L, lift, "Repair", MaintenanceStatus.PENDING);
        saved.setAssignedTechnician(tech);
        when(maintenanceRepository.saveAndFlush(any(Maintenance.class))).thenReturn(saved);

        MaintenanceResponse result = maintenanceService.createMaintenance("admin", request);

        assertThat(result.getAssignedTechnician()).isNotNull();
    }

    @Test
    void updateMaintenance_setsStartedAtWhenStatusChangesToInProgress() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        Maintenance existing = createMaintenance(20L, lift, "Task", MaintenanceStatus.PENDING);
        existing.setStartedAt(null);
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Task");
        request.setStatus(MaintenanceStatus.IN_PROGRESS);

        when(maintenanceRepository.saveAndFlush(any(Maintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        maintenanceService.updateMaintenance("admin", 20L, request);

        assertThat(existing.getStartedAt()).isNotNull();
    }

    @Test
    void updateMaintenance_setsCompletedAtWhenStatusChangesToDone() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        Maintenance existing = createMaintenance(20L, lift, "Task", MaintenanceStatus.IN_PROGRESS);
        existing.setStartedAt(LocalDateTime.now().minusHours(2));
        existing.setCompletedAt(null);
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Task");
        request.setStatus(MaintenanceStatus.DONE);

        when(maintenanceRepository.saveAndFlush(any(Maintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        maintenanceService.updateMaintenance("admin", 20L, request);

        assertThat(existing.getCompletedAt()).isNotNull();
    }

    @Test
    void updateMaintenance_doesNotOverwriteExistingStartedAt() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        LocalDateTime originalStartedAt = LocalDateTime.now().minusDays(1);
        Maintenance existing = createMaintenance(20L, lift, "Task", MaintenanceStatus.PENDING);
        existing.setStartedAt(originalStartedAt);
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Task");
        request.setStatus(MaintenanceStatus.IN_PROGRESS);

        when(maintenanceRepository.saveAndFlush(any(Maintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        maintenanceService.updateMaintenance("admin", 20L, request);

        assertThat(existing.getStartedAt()).isEqualTo(originalStartedAt);
    }

    @Test
    void deleteMaintenance_deletesMaintenance() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        Maintenance m = createMaintenance(20L, lift, "Task", MaintenanceStatus.PENDING);
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(m));

        maintenanceService.deleteMaintenance("admin", 20L);

        verify(maintenanceRepository).delete(m);
        verify(maintenanceRepository).flush();
    }

    @Test
    void deleteMaintenance_throwsWhenDocumentsAttached() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        Maintenance m = createMaintenance(20L, lift, "Task", MaintenanceStatus.PENDING);
        when(maintenanceRepository.findById(20L)).thenReturn(Optional.of(m));
        doNothing().when(maintenanceRepository).delete(m);
        doThrow(new DataIntegrityViolationException("FK")).when(maintenanceRepository).flush();

        assertThatThrownBy(() -> maintenanceService.deleteMaintenance("admin", 20L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("documents are attached");
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

    private Lift createLift(Long id, String serialNumber) {
        Lift lift = new Lift();
        lift.setId(id);
        lift.setSerialNumber(serialNumber);
        lift.setStatus(LiftStatus.CREATED);
        return lift;
    }

    private Maintenance createMaintenance(Long id, Lift lift, String title, MaintenanceStatus status) {
        Maintenance m = new Maintenance();
        m.setId(id);
        m.setLift(lift);
        m.setTitle(title);
        m.setStatus(status);
        m.setRequestedAt(LocalDateTime.now());
        return m;
    }
}
