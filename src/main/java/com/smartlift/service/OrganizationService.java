package com.smartlift.service;

import com.smartlift.dto.OrganizationRequest;
import com.smartlift.dto.OrganizationResponse;
import java.util.List;

public interface OrganizationService {

    List<OrganizationResponse> getAllOrganizations();

    OrganizationResponse getOrganizationById(Long id);

    OrganizationResponse createOrganization(OrganizationRequest request);

    OrganizationResponse updateOrganization(Long id, OrganizationRequest request);

    void deleteOrganization(Long id);
}
