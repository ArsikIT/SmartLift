package com.smartlift.service.impl;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.User;
import com.smartlift.model.enums.MaintenanceStatus;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.MaintenanceRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.MaintenanceService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final LiftRepository liftRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getAllMaintenances(String currentUsername, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        return maintenanceRepository.findAllByOrganizationId(user.getOrganization().getId(), pageable)
                .map(SmartLiftMapper::toMaintenanceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getMaintenancesByLiftId(String currentUsername, Long liftId, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = getLiftOrThrow(liftId);
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());
        return maintenanceRepository.findAllByLiftId(liftId, pageable)
                .map(SmartLiftMapper::toMaintenanceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getMaintenanceById(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        Maintenance maintenance = getOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(maintenance.getLift(), user.getOrganization().getId());
        return SmartLiftMapper.toMaintenanceResponse(maintenance);
    }

    @Override
    public MaintenanceResponse createMaintenance(String currentUsername, MaintenanceRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = getLiftOrThrow(request.getLiftId());
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());

        Maintenance maintenance = new Maintenance();
        maintenance.setRequestedAt(LocalDateTime.now());
        applyRequest(maintenance, request, lift);
        return saveAndMap(maintenance);
    }

    @Override
    public MaintenanceResponse updateMaintenance(String currentUsername, Long id, MaintenanceRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Maintenance maintenance = getOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(maintenance.getLift(), user.getOrganization().getId());

        MaintenanceStatus previousStatus = maintenance.getStatus();
        Lift lift = getLiftOrThrow(request.getLiftId());
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());

        applyRequest(maintenance, request, lift);
        applyStatusTimestamps(maintenance, previousStatus);
        return saveAndMap(maintenance);
    }

    @Override
    public void deleteMaintenance(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        Maintenance maintenance = getOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(maintenance.getLift(), user.getOrganization().getId());
        try {
            maintenanceRepository.delete(maintenance);
            maintenanceRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Maintenance cannot be deleted while documents are attached");
        }
    }

    private void applyRequest(Maintenance maintenance, MaintenanceRequest request, Lift lift) {
        maintenance.setLift(lift);
        maintenance.setTitle(request.getTitle().trim());
        maintenance.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            maintenance.setStatus(request.getStatus());
        }
        maintenance.setAssignedTechnician(resolveUser(request.getAssignedTechnicianId()));
        maintenance.setRequestedBy(resolveUser(request.getRequestedByUserId()));
    }

    private void applyStatusTimestamps(Maintenance maintenance, MaintenanceStatus previousStatus) {
        MaintenanceStatus newStatus = maintenance.getStatus();
        if (previousStatus != newStatus) {
            if (newStatus == MaintenanceStatus.IN_PROGRESS && maintenance.getStartedAt() == null) {
                maintenance.setStartedAt(LocalDateTime.now());
            }
            if (newStatus == MaintenanceStatus.DONE && maintenance.getCompletedAt() == null) {
                maintenance.setCompletedAt(LocalDateTime.now());
            }
        }
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Lift getLiftOrThrow(Long liftId) {
        return liftRepository.findById(liftId)
                .orElseThrow(() -> new ResourceNotFoundException("Lift not found: " + liftId));
    }

    private MaintenanceResponse saveAndMap(Maintenance maintenance) {
        Maintenance saved = maintenanceRepository.saveAndFlush(maintenance);
        return SmartLiftMapper.toMaintenanceResponse(saved);
    }

    private Maintenance getOrThrow(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found: " + id));
    }
}
