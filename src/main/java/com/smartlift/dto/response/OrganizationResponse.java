package com.smartlift.dto.response;

import com.smartlift.model.enums.OrganizationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrganizationResponse {

    private final Long id;
    private final String name;
    private final OrganizationType type;
    private final String address;
    private final String contactEmail;
    private final String contactPhone;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
