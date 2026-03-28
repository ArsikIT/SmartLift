package com.smartlift.service.impl;

import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.LiftResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Lift;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.OrganizationRepository;
import com.smartlift.service.LiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LiftServiceImpl implements LiftService {

    private final LiftRepository liftRepository;
    private final OrganizationRepository organizationRepository;
    private final SecurityContextHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<LiftResponse> getAllLifts(String currentUsername, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        return liftRepository.findAllByOrganizationId(user.getOrganization().getId(), pageable)
                .map(SmartLiftMapper::toLiftResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LiftResponse getLiftById(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = getDetailedLiftOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());
        return SmartLiftMapper.toLiftResponse(lift);
    }

    @Override
    @Transactional(readOnly = true)
    public LiftResponse getLiftBySerialNumber(String currentUsername, String serialNumber) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = liftRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Lift not found with serial number: " + serialNumber));
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());
        return SmartLiftMapper.toLiftResponse(lift);
    }

    @Override
    public LiftResponse createLift(String currentUsername, LiftRequest request) {
        securityHelper.resolveUser(currentUsername);
        Lift lift = new Lift();
        applyLiftRequest(lift, request);
        return saveAndMap(lift);
    }

    @Override
    public LiftResponse updateLift(String currentUsername, Long id, LiftRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift existingLift = getLiftOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(existingLift, user.getOrganization().getId());
        applyLiftRequest(existingLift, request);
        return saveAndMap(existingLift);
    }

    @Override
    public void deleteLift(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift existingLift = getLiftOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(existingLift, user.getOrganization().getId());
        try {
            liftRepository.delete(existingLift);
            liftRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Lift cannot be deleted while related events, maintenance, or documents exist");
        }
    }

    private void applyLiftRequest(Lift lift, LiftRequest request) {
        validateBusinessRules(request, lift.getId());
        lift.setSerialNumber(request.getSerialNumber());
        lift.setModel(request.getModel());
        lift.setManufacturer(request.getManufacturer());
        if (request.getStatus() != null && lift.getId() != null) {
            throw new BadRequestException("Lift status can only be changed through events");
        }
        lift.setManufacturerOrganization(resolveOrganization(
                request.getManufacturerOrganizationId(),
                OrganizationType.MANUFACTURER,
                "manufacturerOrganizationId"
        ));
        lift.setServiceOrganization(resolveOrganization(
                request.getServiceOrganizationId(),
                OrganizationType.SERVICE,
                "serviceOrganizationId"
        ));
        lift.setManagementOrganization(resolveOrganization(
                request.getManagementOrganizationId(),
                OrganizationType.MANAGEMENT,
                "managementOrganizationId"
        ));
    }

    private void validateBusinessRules(LiftRequest request, Long currentLiftId) {
        boolean serialExists = liftRepository.findBySerialNumber(request.getSerialNumber())
                .map(existingLift -> !existingLift.getId().equals(currentLiftId))
                .orElse(false);
        if (serialExists) {
            throw new ConflictException("Lift serial number already exists");
        }
    }

    private Organization resolveOrganization(Long organizationId, OrganizationType expectedType, String fieldName) {
        if (organizationId == null) {
            return null;
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        if (organization.getType() != expectedType) {
            throw new BadRequestException(fieldName + " must reference a " + expectedType + " organization");
        }
        return organization;
    }

    private LiftResponse saveAndMap(Lift lift) {
        try {
            Lift savedLift = liftRepository.saveAndFlush(lift);
            return SmartLiftMapper.toLiftResponse(getDetailedLiftOrThrow(savedLift.getId()));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Lift serial number already exists");
        }
    }

    private Lift getDetailedLiftOrThrow(Long id) {
        return liftRepository.findDetailedById(id)
                .orElseThrow(notFound(id));
    }

    private Lift getLiftOrThrow(Long id) {
        return liftRepository.findById(id)
                .orElseThrow(notFound(id));
    }

    private java.util.function.Supplier<ResourceNotFoundException> notFound(Long id) {
        return () -> new ResourceNotFoundException("Lift not found: " + id);
    }
}
