package com.smartlift.service.impl;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Organization;
import com.smartlift.repository.OrganizationRepository;
import com.smartlift.service.OrganizationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(SmartLiftMapper::toOrganizationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(Long id) {
        return SmartLiftMapper.toOrganizationResponse(getOrThrow(id));
    }

    @Override
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        Organization org = new Organization();
        applyRequest(org, request);
        return saveAndMap(org);
    }

    @Override
    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization org = getOrThrow(id);
        applyRequest(org, request);
        return saveAndMap(org);
    }

    @Override
    public void deleteOrganization(Long id) {
        Organization org = getOrThrow(id);
        try {
            organizationRepository.delete(org);
            organizationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Organization cannot be deleted while referenced by users or lifts");
        }
    }

    private void applyRequest(Organization org, OrganizationRequest request) {
        validateUniqueName(request.getName(), org.getId());
        org.setName(request.getName().trim());
        org.setType(request.getType());
        org.setAddress(request.getAddress());
        org.setContactEmail(request.getContactEmail());
        org.setContactPhone(request.getContactPhone());
    }

    private void validateUniqueName(String name, Long currentId) {
        organizationRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Organization name already exists: " + name);
            }
        });
    }

    private OrganizationResponse saveAndMap(Organization org) {
        try {
            Organization saved = organizationRepository.saveAndFlush(org);
            return SmartLiftMapper.toOrganizationResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Organization name already exists");
        }
    }

    private Organization getOrThrow(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }
}
