package com.smartlift.service;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import java.util.List;

public interface OrganizationService {

    List<OrganizationResponse> getAllOrganizations();

    OrganizationResponse getOrganizationById(Long id);

    OrganizationResponse createOrganization(OrganizationRequest request);

    OrganizationResponse updateOrganization(Long id, OrganizationRequest request);

    void deleteOrganization(Long id);
}
