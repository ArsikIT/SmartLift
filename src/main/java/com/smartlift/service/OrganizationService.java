package com.smartlift.service;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;

public interface OrganizationService {

    OrganizationResponse getMyOrganization(String currentUsername);

    OrganizationResponse updateMyOrganization(String currentUsername, OrganizationRequest request);
}
