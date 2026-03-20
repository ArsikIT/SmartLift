package com.smartlift.dto;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final boolean enabled;
    private final OrganizationSummaryResponse organization;
    private final Set<String> roles;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
