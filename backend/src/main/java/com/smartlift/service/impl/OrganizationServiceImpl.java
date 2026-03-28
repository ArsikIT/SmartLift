package com.smartlift.service.impl;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.repository.OrganizationRepository;
import com.smartlift.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SecurityContextHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getMyOrganization(String currentUsername) {
        User user = securityHelper.resolveUser(currentUsername);
        return SmartLiftMapper.toOrganizationResponse(user.getOrganization());
    }

    @Override
    public OrganizationResponse updateMyOrganization(String currentUsername, OrganizationRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Organization org = user.getOrganization();

        validateUniqueName(request.getName(), org.getId());
        org.setName(request.getName().trim());
        org.setAddress(request.getAddress());
        org.setContactEmail(request.getContactEmail());
        org.setContactPhone(request.getContactPhone());

        Organization saved = organizationRepository.saveAndFlush(org);
        return SmartLiftMapper.toOrganizationResponse(saved);
    }

    private void validateUniqueName(String name, Long currentId) {
        organizationRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Organization name already exists: " + name);
            }
        });
    }
}
