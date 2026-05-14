package com.smartlift.dto.response;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final Long userId;
    private final String token;
    private final String username;
    private final Set<String> roles;
    private final OrganizationSummaryResponse organization;
}
