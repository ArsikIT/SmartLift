package com.smartlift.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryResponse {

    private final Long id;
    private final String username;
    private final String email;
}
