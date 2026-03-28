package com.smartlift.service;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {

    Page<OrganizationResponse> getAllOrganizations(Pageable pageable);

    OrganizationResponse getMyOrganization(String currentUsername);

    OrganizationResponse updateMyOrganization(String currentUsername, OrganizationRequest request);
}
