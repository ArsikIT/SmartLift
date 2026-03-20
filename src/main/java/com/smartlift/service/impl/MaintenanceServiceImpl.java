package com.smartlift.service.impl;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.enums.MaintenanceStatus;
import com.smartlift.model.User;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.MaintenanceRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.MaintenanceService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final LiftRepository liftRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getAllMaintenances() {
        return maintenanceRepository.findAll().stream()
                .map(SmartLiftMapper::toMaintenanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMaintenancesByLiftId(Long liftId) {
        ensureLiftExists(liftId);
        return maintenanceRepository.findAllByLiftId(liftId).stream()
                .map(SmartLiftMapper::toMaintenanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getMaintenanceById(Long id) {
        return SmartLiftMapper.toMaintenanceResponse(getOrThrow(id));
    }

    @Override
    public MaintenanceResponse createMaintenance(MaintenanceRequest request) {
        Maintenance maintenance = new Maintenance();
        maintenance.setRequestedAt(LocalDateTime.now());
        applyRequest(maintenance, request);
        return saveAndMap(maintenance);
    }

    @Override
    public MaintenanceResponse updateMaintenance(Long id, MaintenanceRequest request) {
        Maintenance maintenance = getOrThrow(id);
        MaintenanceStatus previousStatus = maintenance.getStatus();
        applyRequest(maintenance, request);
        applyStatusTimestamps(maintenance, previousStatus);
        return saveAndMap(maintenance);
    }

    @Override
    public void deleteMaintenance(Long id) {
        Maintenance maintenance = getOrThrow(id);
        try {
            maintenanceRepository.delete(maintenance);
            maintenanceRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Maintenance cannot be deleted while documents are attached");
        }
    }

    private void applyRequest(Maintenance maintenance, MaintenanceRequest request) {
        Lift lift = liftRepository.findById(request.getLiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Lift not found: " + request.getLiftId()));

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

    private MaintenanceResponse saveAndMap(Maintenance maintenance) {
        Maintenance saved = maintenanceRepository.saveAndFlush(maintenance);
        return SmartLiftMapper.toMaintenanceResponse(saved);
    }

    private void ensureLiftExists(Long liftId) {
        if (!liftRepository.existsById(liftId)) {
            throw new ResourceNotFoundException("Lift not found: " + liftId);
        }
    }

    private Maintenance getOrThrow(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found: " + id));
    }
}
