package com.smartlift.dto.response;

import com.smartlift.model.enums.LiftStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiftResponse {

    private final Long id;
    private final String serialNumber;
    private final String model;
    private final String manufacturer;
    private final LiftStatus status;
    private final OrganizationSummaryResponse manufacturerOrganization;
    private final OrganizationSummaryResponse serviceOrganization;
    private final OrganizationSummaryResponse managementOrganization;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
