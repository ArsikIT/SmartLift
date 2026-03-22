package com.smartlift.service;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {

    Page<OrganizationResponse> getAllOrganizations(Pageable pageable);

    OrganizationResponse getOrganizationById(Long id);

    OrganizationResponse createOrganization(OrganizationRequest request);

    OrganizationResponse updateOrganization(Long id, OrganizationRequest request);

    void deleteOrganization(Long id);
}
