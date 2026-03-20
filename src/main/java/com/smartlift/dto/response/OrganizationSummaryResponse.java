package com.smartlift.dto.response;

import com.smartlift.model.enums.OrganizationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrganizationSummaryResponse {

    private final Long id;
    private final String name;
    private final OrganizationType type;
}
